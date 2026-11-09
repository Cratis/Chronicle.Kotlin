// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.imports

import io.cratis.chronicle.eventSequences.EventForEventSourceId
import kotlinx.coroutines.flow.Flow

/**
 * An import session that events are added to in chunks, for producers that are not a single stream:
 * several producers taking turns, or a resumable extract-transform-load job.
 *
 * Events are cut into chunks within the kernel's limits, and a bounded number of chunks are sent at the
 * same time ([ImportOptions.maxChunksInFlight]). Nothing is committed before [complete]. Closing a session
 * that was not completed leaves it open on the kernel, so it can be reattached by its [importId]; the
 * kernel abandons it once its staging retention runs out.
 */
interface IEventImport {
    /** The identifier of the import, to reattach to it later. */
    val importId: ImportId

    /**
     * How many events the kernel already held, in order and without a gap, when this session was
     * attached. A producer that reattaches continues after this many events; a new session starts at zero.
     */
    val eventsAcknowledged: Long

    /** The progress of the import as the kernel reports it, for as long as it is collected. */
    val progress: Flow<ImportProgress>

    /**
     * Adds events to the import, reading [events] lazily: collecting pauses while the in-flight bound is
     * reached, so the source is never read into memory as a whole.
     *
     * @param events The events, in the order they are to be committed.
     * @throws ImportWasAbandoned The import was abandoned, before or while adding.
     * @throws ImportChunkRefused The kernel refused a chunk sent earlier.
     */
    suspend fun add(events: Flow<EventForEventSourceId>)

    /**
     * Adds events from a lazy sequence to the import, reading it as the chunks are sent.
     *
     * @param events The events, in the order they are to be committed.
     */
    suspend fun add(events: Sequence<EventForEventSourceId>)

    /**
     * Adds events to the import.
     *
     * @param events The events, in the order they are to be committed.
     */
    suspend fun add(events: Iterable<EventForEventSourceId>)

    /**
     * Completes the import: sends what is left, waits for every chunk to be acknowledged, closes staging
     * and starts the commit.
     *
     * @return The [ImportResult], once committed unless [ImportOptions.waitForCommit] is `false`.
     * @throws ImportWasAbandoned The import was abandoned.
     * @throws ImportChunkRefused The kernel refused a chunk.
     */
    suspend fun complete(): ImportResult

    /**
     * Abandons the import, discarding everything staged. Nothing is committed.
     *
     * No chunk is sent once abandoning begins. An [add] in progress is stopped rather than waited for, so
     * a source that never produces again cannot hold the abandon up; it ends with [ImportWasAbandoned].
     * Chunks already handed to the kernel are waited for, so none is staged after the import is abandoned.
     *
     * @return The [ImportResult].
     */
    suspend fun abandon(): ImportResult

    /**
     * Releases the session. A session that was not completed or abandoned stays open on the kernel and can
     * be reattached by its [importId]; only the chunks already sent are waited for.
     */
    suspend fun close()
}
