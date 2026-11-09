// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.imports

import Cratis.Chronicle.Contracts.Imports.ImportsOuterClass as Contracts
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.toBclGuid
import io.grpc.Status
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock

/**
 * An [IEventImport] that cuts events into chunks within the kernel's limits and keeps a bounded number
 * of them in flight.
 *
 * Reading the source and sending chunks are separate: the caller's coroutine reads and buffers, and
 * each chunk is sent from [sendContext]. A chunk is acknowledged by its index, so sending it again after
 * a transient failure is safe.
 *
 * @param calls The kernel's `Imports` service.
 * @param importId The import.
 * @param options The [ImportOptions].
 * @param maxChunkEvents The most events the kernel accepts in one chunk; zero or less means no limit.
 * @param maxChunkBytes The most bytes of event content the kernel accepts in one chunk; zero or less means no limit.
 * @param convert Converts an event to its wire form.
 * @param acknowledgedChunks How many chunks the kernel already holds without a gap, when reattaching.
 * @param acknowledgedEvents How many events those chunks carry, when reattaching.
 * @param retryDelay The delay before sending a chunk again, by attempt.
 * @param pollInterval The interval between reads of the import while waiting for its commit.
 * @param sendContext Where chunks are sent from.
 */
internal class EventImport(
    private val calls: ImportCalls,
    private val eventStore: String,
    private val namespace: String,
    override val importId: ImportId,
    private val options: ImportOptions,
    maxChunkEvents: Int,
    maxChunkBytes: Int,
    private val convert: suspend (EventForEventSourceId) -> Contracts.ImportEvent,
    acknowledgedChunks: Int = 0,
    acknowledgedEvents: Long = 0,
    private val retryDelay: (Int) -> Duration = { attempt -> minOf(5000L, 200L * attempt * attempt).milliseconds },
    private val pollInterval: Duration = 500.milliseconds,
    sendContext: CoroutineContext = Dispatchers.Default,
) : IEventImport {
    private val maxEventsPerChunk = maxOf(1, minOf(maxChunkEvents.orUnlimited(), options.maxEventsPerChunk ?: Int.MAX_VALUE))
    private val maxBytesPerChunk = maxChunkBytes.orUnlimited()
    private val adding = Mutex()
    private val inFlight = Semaphore(maxOf(1, options.maxChunksInFlight))
    private val sending = CoroutineScope(SupervisorJob() + sendContext)
    private val lock = Any()
    private val sends = mutableListOf<Job>()
    private val buffer = mutableListOf<Contracts.ImportEvent>()
    private var bufferBytes = 0
    private var nextChunkIndex = acknowledgedChunks
    private var nextEventIndex = acknowledgedEvents
    private var working: Job? = null
    private var failure: Throwable? = null

    @Volatile
    private var abandoned = false

    @Volatile
    private var completed = false

    override val eventsAcknowledged: Long = acknowledgedEvents

    override val progress: Flow<ImportProgress>
        get() = calls.observe(
            Contracts.ObserveImportRequest.newBuilder()
                .setEventStore(eventStore)
                .setNamespace(namespace)
                .setImportId(importId.value.toBclGuid())
                .build(),
        ).map { it.toProgress() }

    override suspend fun add(events: Flow<EventForEventSourceId>) = whileNotAbandoned { events.collect { buffer(it) } }

    override suspend fun add(events: Sequence<EventForEventSourceId>) = whileNotAbandoned { events.forEach { buffer(it) } }

    override suspend fun add(events: Iterable<EventForEventSourceId>) = add(events.asSequence())

    override suspend fun complete(): ImportResult = whileNotAbandoned {
        flush()
        awaitSends()
        throwIfFailed()
        throwIfAbandoned()

        val status = calls.complete(
            Contracts.CompleteImportRequest.newBuilder()
                .setEventStore(eventStore)
                .setNamespace(namespace)
                .setImportId(importId.value.toBclGuid())
                .setExpectedChunks(nextChunkIndex)
                .setExpectedEvents(nextEventIndex)
                .build(),
        )
        completed = true

        if (status.error.isEmpty() && options.waitForCommit) waitForCommit() else read() ?: resultFrom(status)
    }

    override suspend fun abandon(): ImportResult {
        // No chunk is sent once this is set, and an add in progress is stopped rather than waited for: it
        // may be reading a source that never produces again.
        val reading = synchronized(lock) {
            abandoned = true
            working
        }
        reading?.cancel(AbandonedWhileWorking())

        // Chunks already handed to the kernel settle first, so none is staged after the import is abandoned.
        awaitSends()
        val status = calls.abandon(
            Contracts.AbandonImportRequest.newBuilder()
                .setEventStore(eventStore)
                .setNamespace(namespace)
                .setImportId(importId.value.toBclGuid())
                .build(),
        )
        completed = true
        return read() ?: resultFrom(status)
    }

    override suspend fun close() {
        // An import that was not completed stays open on the kernel so it can be reattached; only wait for
        // what is in flight.
        if (!completed) awaitSends()
        sending.cancel()
    }

    private suspend fun <T> whileNotAbandoned(work: suspend () -> T): T {
        throwIfAbandoned()
        try {
            return adding.withLock {
                coroutineScope {
                    val job = checkNotNull(coroutineContext[Job])
                    synchronized(lock) {
                        if (abandoned) throw ImportWasAbandoned(importId)
                        working = job
                    }
                    try {
                        work()
                    } finally {
                        synchronized(lock) { if (working === job) working = null }
                    }
                }
            }
        } catch (e: CancellationException) {
            // Abandoning cancels only the work, not the caller; a caller that was itself cancelled is told so.
            if (abandoned && currentCoroutineContext().isActive) throw ImportWasAbandoned(importId)
            throw e
        }
    }

    private suspend fun buffer(event: EventForEventSourceId) {
        throwIfFailed()
        throwIfAbandoned()
        val converted = convert(event)
        val bytes = converted.sizeInBytes()
        if (buffer.isNotEmpty() && (buffer.size >= maxEventsPerChunk || bufferBytes.toLong() + bytes > maxBytesPerChunk)) flush()

        buffer.add(converted)
        bufferBytes += bytes
    }

    private suspend fun flush() {
        if (buffer.isEmpty()) return

        val events = buffer.toList()
        val chunkIndex = nextChunkIndex
        val firstEventIndex = nextEventIndex

        // Reading pauses here while the in-flight bound is reached.
        inFlight.acquire()
        synchronized(lock) {
            // Checked and registered together, so abandon sees every chunk sent before it and none after it.
            if (abandoned) {
                inFlight.release()
                throw ImportWasAbandoned(importId)
            }

            nextChunkIndex++
            nextEventIndex += events.size
            buffer.clear()
            bufferBytes = 0
            sends.removeAll { it.isCompleted }
            sends.add(sending.launch { send(chunkIndex, firstEventIndex, events) })
        }
    }

    private suspend fun send(chunkIndex: Int, firstEventIndex: Long, events: List<Contracts.ImportEvent>) {
        val request = Contracts.AddImportChunkRequest.newBuilder()
            .setEventStore(eventStore)
            .setNamespace(namespace)
            .setImportId(importId.value.toBclGuid())
            .setChunkIndex(chunkIndex)
            .setFirstEventIndex(firstEventIndex)
            .addAllEvents(events)
            .build()
        try {
            var attempt = 1
            while (true) {
                try {
                    val result = calls.addChunk(request)
                    if (result.error.isNotEmpty()) throw ImportChunkRefused(chunkIndex, result.error)
                    return
                } catch (e: Exception) {
                    if (statusCodeOf(e) !in transientStatusCodes || attempt >= options.maxChunkAttempts) throw e

                    // Sending the chunk again is safe: the kernel stages a chunk under its index once.
                    delay(retryDelay(attempt))
                    attempt++
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            synchronized(lock) { if (failure == null) failure = e }
        } finally {
            inFlight.release()
        }
    }

    private suspend fun awaitSends() = synchronized(lock) { sends.toList() }.joinAll()

    private fun throwIfAbandoned() {
        if (abandoned) throw ImportWasAbandoned(importId)
    }

    private fun throwIfFailed() {
        synchronized(lock) { failure }?.let { throw it }
    }

    private suspend fun waitForCommit(): ImportResult {
        while (true) {
            val result = read()
            if (result != null && result.state.isTerminal) return result
            delay(pollInterval)
        }
    }

    private suspend fun read(): ImportResult? = calls.get(
        Contracts.GetImportRequest.newBuilder()
            .setEventStore(eventStore)
            .setNamespace(namespace)
            .setImportId(importId.value.toBclGuid())
            .build(),
    )?.toResult()

    private fun resultFrom(status: Contracts.AbandonImportResponse) =
        ImportResult(importId, status.state.toClient(), failureReason = status.error.ifEmpty { null })

    private fun Int.orUnlimited(): Int = if (this <= 0) Int.MAX_VALUE else this

    /** Cancels the work of an abandoned import; the caller is told [ImportWasAbandoned] instead. */
    private class AbandonedWhileWorking : CancellationException("The import was abandoned.")

    private companion object {
        val transientStatusCodes = setOf(
            Status.Code.UNAVAILABLE,
            Status.Code.DEADLINE_EXCEEDED,
            Status.Code.RESOURCE_EXHAUSTED,
            Status.Code.ABORTED,
            Status.Code.INTERNAL,
        )
    }
}
