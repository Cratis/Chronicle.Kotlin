// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.imports.IEventImport
import io.cratis.chronicle.imports.ImportId
import io.cratis.chronicle.imports.ImportProgress
import io.cratis.chronicle.imports.ImportResult
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Flow as JdkFlow
import java.util.function.Consumer
import java.util.stream.Stream
import kotlin.streams.asSequence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.future.future
import kotlinx.coroutines.jdk9.asFlow
import kotlinx.coroutines.launch

/**
 * An import session for Java: the [IEventImport] with every suspending call returned as a
 * [CompletableFuture], so a Java producer can add events from several places before completing.
 *
 * Cancelling a returned future cancels the work behind it: an add stops reading its source, and the
 * session stays open on the kernel so it can be reattached by its [importId].
 *
 * @param session The session underneath.
 */
class JavaEventImport(private val session: IEventImport) {
    private val scope = CoroutineScope(Dispatchers.IO)

    /** The suspending session underneath, for Kotlin callers. */
    fun unwrap(): IEventImport = session

    /** The identifier of the import, to reattach to it later. */
    val importId: ImportId get() = session.importId

    /** How many events the kernel already held when this session was attached. */
    val eventsAcknowledged: Long get() = session.eventsAcknowledged

    /**
     * Adds events from a reactive publisher, requesting them as chunks are sent.
     *
     * @param events The events, in the order they are to be committed.
     * @return Completes once the events are handed to the kernel within the in-flight bound.
     */
    fun add(events: JdkFlow.Publisher<EventForEventSourceId>): CompletableFuture<Void?> =
        scope.future { session.add(events.asFlow()); null }

    /**
     * Adds events from a stream, reading it lazily as chunks are sent. The stream is closed afterwards.
     *
     * @param events The events, in the order they are to be committed.
     * @return Completes once the events are handed to the kernel within the in-flight bound.
     */
    fun add(events: Stream<EventForEventSourceId>): CompletableFuture<Void?> =
        scope.future { events.use { session.add(it.asSequence()) }; null }

    /**
     * Adds events from a list.
     *
     * @param events The events, in the order they are to be committed.
     * @return Completes once the events are handed to the kernel within the in-flight bound.
     */
    fun add(events: List<EventForEventSourceId>): CompletableFuture<Void?> =
        scope.future { session.add(events); null }

    /** Completes the import; the future holds the [ImportResult]. */
    fun complete(): CompletableFuture<ImportResult> = scope.future { session.complete() }

    /** Abandons the import, discarding everything staged; the future holds the [ImportResult]. */
    fun abandon(): CompletableFuture<ImportResult> = scope.future { session.abandon() }

    /** Releases the session; one that was not completed stays open on the kernel to be reattached. */
    fun close(): CompletableFuture<Void?> = scope.future { session.close(); null }

    /**
     * Calls [onProgress] with the progress the kernel reports, until the returned handle is closed.
     *
     * @param onProgress Receives each progress report.
     * @return Closing it stops the reports.
     */
    fun onProgress(onProgress: Consumer<ImportProgress>): AutoCloseable {
        val job = scope.launch { session.progress.catch { }.collect { onProgress.accept(it) } }
        return AutoCloseable { job.cancel() }
    }
}
