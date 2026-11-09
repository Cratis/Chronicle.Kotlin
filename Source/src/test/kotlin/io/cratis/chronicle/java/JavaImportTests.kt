// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import Cratis.Chronicle.Contracts.Imports.ImportsOuterClass as Contracts
import Cratis.Chronicle.Contracts.Sequences.EventSequencesGrpcKt
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.EventSequence
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.imports.FakeImportCalls
import io.cratis.chronicle.imports.ImportProgress
import io.cratis.chronicle.imports.ImportState
import io.mockk.mockk
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.SubmissionPublisher
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@EventType
data class JavaImportedEvent(val value: Int)

/**
 * Java imports without coroutines: from a reactive publisher or a stream, as a [CompletableFuture], with
 * progress through a callback and cancellation through the future.
 */
class JavaImportTests {
    private val calls = FakeImportCalls(maxChunkEvents = 2)
    private val eventLog = BlockingEventSequence(
        EventSequence(EventSequenceId.eventLog, "store", "default", mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>())
            .also { it.importCalls = calls },
    )

    private fun event(value: Int) = EventForEventSourceId("source-$value", JavaImportedEvent(value))

    @Test
    fun `events from a stream are imported, and the stream is closed`() {
        var closed = false
        val stream = Stream.of(event(1), event(2), event(3)).onClose { closed = true }

        val result = eventLog.importEvents(stream).get(10, TimeUnit.SECONDS)

        assertTrue(result.isSuccess)
        assertEquals(listOf(2, 1), calls.chunks.sortedBy { it.chunkIndex }.map { it.eventsCount })
        assertTrue(closed)
    }

    @Test
    fun `events from a publisher are imported as they are published`() {
        val publisher = SubmissionPublisher<EventForEventSourceId>()

        val future = eventLog.importEvents(publisher)
        while (publisher.numberOfSubscribers == 0) Thread.onSpinWait()
        (1..5).forEach { publisher.submit(event(it)) }
        publisher.close()
        val result = future.get(10, TimeUnit.SECONDS)

        assertTrue(result.isSuccess)
        assertEquals(5, calls.chunks.sumOf { it.eventsCount })
    }

    @Test
    fun `progress reaches the callback`() {
        runBlocking { calls.progressUpdates.emit(calls.progress(Contracts.ImportState.Staging) { setEventsReceived(1) }) }
        val reported = CopyOnWriteArrayList<ImportProgress>()
        calls.onChunk = { request ->
            while (reported.isEmpty()) kotlinx.coroutines.yield()
            FakeImportCalls.accepted(request)
        }

        eventLog.importEvents(Stream.of(event(1)), onProgress = { reported.add(it) }).get(10, TimeUnit.SECONDS)

        assertEquals(ImportState.Staging, reported.first().state)
    }

    @Test
    fun `cancelling the future stops the import without completing it`() {
        val sending = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        calls.onChunk = { request ->
            sending.complete(Unit)
            release.await()
            FakeImportCalls.accepted(request)
        }
        val publisher = SubmissionPublisher<EventForEventSourceId>()
        val future: CompletableFuture<*> = eventLog.importEvents(publisher)
        while (publisher.numberOfSubscribers == 0) Thread.onSpinWait()
        (1..3).forEach { publisher.submit(event(it)) }
        runBlocking { sending.await() }

        future.cancel(true)
        release.complete(Unit)

        assertThrows<CancellationException> { future.get(10, TimeUnit.SECONDS) }
        assertTrue(calls.completed.isEmpty())
        assertTrue(calls.abandoned.isEmpty())
    }

    @Test
    fun `a session is driven step by step through futures`() {
        val session = eventLog.beginImport().get(10, TimeUnit.SECONDS)

        session.add(listOf(event(1), event(2), event(3))).get(10, TimeUnit.SECONDS)
        val result = session.complete().get(10, TimeUnit.SECONDS)
        session.close().get(10, TimeUnit.SECONDS)

        assertTrue(result.isSuccess)
        assertEquals(3L, calls.completed.single().expectedEvents)
    }
}
