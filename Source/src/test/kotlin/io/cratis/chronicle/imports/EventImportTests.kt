// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.imports

import Cratis.Chronicle.Contracts.Imports.ImportsOuterClass as Contracts
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.grpc.Status
import io.grpc.StatusException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * The import session cuts a lazily read source into chunks within the kernel's limits, keeps a bounded
 * number of them in flight, and reports what the kernel made of them.
 */
class EventImportTests {
    private val calls = FakeImportCalls()

    private fun importOf(
        options: ImportOptions = ImportOptions.default,
        maxChunkEvents: Int = 0,
        maxChunkBytes: Int = 0,
        acknowledgedChunks: Int = 0,
        acknowledgedEvents: Long = 0,
    ) = EventImport(
        calls,
        "store",
        "default",
        ImportId(calls.importId),
        options,
        maxChunkEvents,
        maxChunkBytes,
        { event -> FakeImportCalls.wireEvent(event.eventSourceId) },
        acknowledgedChunks,
        acknowledgedEvents,
        retryDelay = { 1.milliseconds },
        pollInterval = 1.milliseconds,
        sendContext = Dispatchers.Default,
    )

    private fun events(count: Int, prefix: String = "e") = (0 until count).map { EventForEventSourceId("$prefix$it", Any()) }

    @Test
    fun `events are cut into chunks no larger than the kernel's event limit`() = runBlocking {
        val import = importOf(maxChunkEvents = 3)

        import.add(events(7))
        import.complete()

        val chunks = calls.chunks.sortedBy { it.chunkIndex }
        assertEquals(listOf(0, 1, 2), chunks.map { it.chunkIndex })
        assertEquals(listOf(3, 3, 1), chunks.map { it.eventsCount })
        assertEquals(listOf(0L, 3L, 6L), chunks.map { it.firstEventIndex })
        assertEquals((0 until 7).map { "e$it" }, calls.sentContents())
    }

    @Test
    fun `a chunk is cut before it would pass the kernel's byte limit`() = runBlocking {
        val import = importOf(maxChunkBytes = 10)

        import.add(listOf(EventForEventSourceId("aaaa", Any()), EventForEventSourceId("bbbb", Any()), EventForEventSourceId("cccc", Any())))
        import.complete()

        assertEquals(listOf(2, 1), calls.chunks.sortedBy { it.chunkIndex }.map { it.eventsCount })
    }

    @Test
    fun `an event larger than the byte limit still travels, alone`() = runBlocking {
        val import = importOf(maxChunkBytes = 2)

        import.add(listOf(EventForEventSourceId("larger", Any()), EventForEventSourceId("x", Any())))
        import.complete()

        assertEquals(listOf(1, 1), calls.chunks.sortedBy { it.chunkIndex }.map { it.eventsCount })
    }

    @Test
    fun `a smaller chunk size than the kernel's can be asked for`() = runBlocking {
        val import = importOf(ImportOptions(maxEventsPerChunk = 2), maxChunkEvents = 100)

        import.add(events(5))
        import.complete()

        assertEquals(listOf(2, 2, 1), calls.chunks.sortedBy { it.chunkIndex }.map { it.eventsCount })
    }

    @Test
    fun `chunks are cut across adds, and completing names every chunk and event sent`() = runBlocking {
        val import = importOf(maxChunkEvents = 4)

        import.add(events(3, "a"))
        import.add(events(3, "b"))
        import.complete()

        assertEquals(listOf(4, 2), calls.chunks.sortedBy { it.chunkIndex }.map { it.eventsCount })
        val complete = calls.completed.single()
        assertEquals(2, complete.expectedChunks)
        assertEquals(6L, complete.expectedEvents)
    }

    @Test
    fun `no more chunks are in flight than the options allow, and the source is read only as they settle`() = runBlocking {
        val release = CompletableDeferred<Unit>()
        calls.onChunk = { request ->
            release.await()
            FakeImportCalls.accepted(request)
        }
        val read = AtomicInteger()
        val import = importOf(ImportOptions(maxChunksInFlight = 2), maxChunkEvents = 1)

        val adding = async(Dispatchers.Default) {
            import.add(flow { repeat(10) { emit(EventForEventSourceId("e$it", Any())) } }.onEach { read.incrementAndGet() })
        }
        withTimeout(5_000) { while (calls.inFlight.get() < 2 || read.get() < 4) yield() }
        delay(50)

        // Two chunks are in flight, a third is waiting for room with the reader: nothing more is read.
        assertEquals(4, read.get())
        assertEquals(2, calls.mostInFlight.get())

        release.complete(Unit)
        adding.await()
        import.complete()
        assertEquals(10, calls.chunks.size)
        assertEquals(2, calls.mostInFlight.get())
    }

    @Test
    fun `a chunk that fails transiently is sent again under the same index`() = runBlocking {
        val attempts = AtomicInteger()
        calls.onChunk = { request ->
            if (attempts.incrementAndGet() < 3) throw StatusException(Status.UNAVAILABLE)
            FakeImportCalls.accepted(request)
        }
        val import = importOf()

        import.add(events(2))
        val result = import.complete()

        assertTrue(result.isSuccess)
        assertEquals(listOf(0, 0, 0), calls.chunks.map { it.chunkIndex })
    }

    @Test
    fun `a chunk that keeps failing fails the import when completing, without completing it`() = runBlocking {
        calls.onChunk = { throw StatusException(Status.UNAVAILABLE) }
        val import = importOf(ImportOptions(maxChunkAttempts = 2))

        import.add(events(1))
        val error = assertThrows<StatusException> { runBlocking { import.complete() } }

        assertEquals(Status.Code.UNAVAILABLE, error.status.code)
        assertEquals(2, calls.chunks.size)
        assertTrue(calls.completed.isEmpty())
    }

    @Test
    fun `a chunk the kernel refuses is reported by its index`() = runBlocking {
        calls.onChunk = { request -> FakeImportCalls.accepted(request).toBuilder().setError("too large").build() }
        val import = importOf()

        import.add(events(1))
        val error = assertThrows<ImportChunkRefused> { runBlocking { import.complete() } }

        assertEquals(0, error.chunkIndex)
        assertTrue(error.message!!.contains("too large"))
    }

    @Test
    fun `rejected events are reported by their index in the import`() = runBlocking {
        calls.reads = {
            calls.progress(Contracts.ImportState.Failed) {
                setEventsStaged(1)
                setRejectedCount(1)
                addRejections(Contracts.ImportEventRejection.newBuilder().setIndex(4).setEventType("legacy-order").addReasons("Missing number").build())
                setFailureReason("1 event was rejected")
            }
        }
        val import = importOf()

        import.add(events(5))
        val result = import.complete()

        assertFalse(result.isSuccess)
        assertEquals(ImportState.Failed, result.state)
        assertEquals(listOf(ImportRejection(4, "legacy-order", listOf("Missing number"))), result.rejections)
        assertEquals("1 event was rejected", result.failureReason)
        assertNull(result.firstSequenceNumber)
    }

    @Test
    fun `completing waits for the commit to end`() = runBlocking {
        val reads = AtomicInteger()
        calls.reads = {
            if (reads.incrementAndGet() < 3) {
                calls.progress(Contracts.ImportState.Committing)
            } else {
                calls.progress(Contracts.ImportState.Completed) {
                    setEventsCommitted(2)
                    setFirstSequenceNumber(10)
                    setLastSequenceNumber(11)
                }
            }
        }
        val import = importOf()

        import.add(events(2))
        val result = import.complete()

        assertEquals(3, reads.get())
        assertEquals(ImportState.Completed, result.state)
        assertEquals(10L, result.firstSequenceNumber)
        assertEquals(11L, result.lastSequenceNumber)
    }

    @Test
    fun `completing without waiting returns what the kernel reads as once the commit has started`() = runBlocking {
        calls.reads = { calls.progress(Contracts.ImportState.Committing) }
        val import = importOf(ImportOptions(waitForCommit = false))

        import.add(events(1))

        assertEquals(ImportState.Committing, import.complete().state)
    }

    @Test
    fun `progress is what the kernel reports`() = runBlocking {
        val import = importOf()
        calls.progressUpdates.emit(calls.progress(Contracts.ImportState.Staging) { setEventsReceived(5); setEventsStaged(4); setRejectedCount(1) })
        calls.progressUpdates.emit(calls.progress(Contracts.ImportState.Completed) { setEventsCommitted(4); setAlreadyAppended(1) })

        val progress = import.progress.take(2).toList()

        assertEquals(
            listOf(
                ImportProgress(ImportId(calls.importId), ImportState.Staging, 5, 4, 0, 0, 1),
                ImportProgress(ImportId(calls.importId), ImportState.Completed, 0, 0, 4, 1, 0),
            ),
            progress,
        )
    }

    @Test
    fun `a reattached session continues after the acknowledged chunks`() = runBlocking {
        val import = importOf(maxChunkEvents = 2, acknowledgedChunks = 3, acknowledgedEvents = 6)

        import.add(events(3))
        import.complete()

        assertEquals(6L, import.eventsAcknowledged)
        assertEquals(listOf(3, 4), calls.chunks.sortedBy { it.chunkIndex }.map { it.chunkIndex })
        assertEquals(listOf(6L, 8L), calls.chunks.sortedBy { it.chunkIndex }.map { it.firstEventIndex })
        assertEquals(5, calls.completed.single().expectedChunks)
        assertEquals(9L, calls.completed.single().expectedEvents)
    }

    @Test
    fun `abandoning stops an add waiting on its source and sends nothing more`() = runBlocking {
        val import = importOf(maxChunkEvents = 1)
        val adding = async(Dispatchers.Default) {
            runCatching {
                import.add(
                    flow {
                        emit(EventForEventSourceId("first", Any()))
                        emit(EventForEventSourceId("second", Any()))
                        awaitCancellation()
                    },
                )
            }
        }
        withTimeout(5_000) { while (calls.chunks.isEmpty()) yield() }
        calls.reads = { calls.progress(Contracts.ImportState.Abandoned) }

        val result = import.abandon()

        assertTrue(adding.await().exceptionOrNull() is ImportWasAbandoned)
        assertEquals(ImportState.Abandoned, result.state)
        assertEquals(1, calls.abandoned.size)
        assertEquals(listOf("first"), calls.sentContents())
        assertThrows<ImportWasAbandoned> { runBlocking { import.add(events(1)) } }
        assertThrows<ImportWasAbandoned> { runBlocking { import.complete() } }
    }

    @Test
    fun `cancelling an add stops reading but leaves the import open to be reattached`() = runBlocking {
        val import = importOf(maxChunkEvents = 1)
        val adding = launch(Dispatchers.Default) {
            import.add(
                flow {
                    emit(EventForEventSourceId("first", Any()))
                    emit(EventForEventSourceId("second", Any()))
                    awaitCancellation()
                },
            )
        }
        withTimeout(5_000) { while (calls.chunks.isEmpty()) yield() }

        adding.cancelAndJoin()
        import.close()

        assertTrue(adding.isCancelled)
        assertTrue(calls.abandoned.isEmpty())
        assertTrue(calls.completed.isEmpty())
        assertEquals(listOf("first"), calls.sentContents())
    }
}
