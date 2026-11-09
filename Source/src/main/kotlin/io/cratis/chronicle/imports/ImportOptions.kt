// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.imports

/**
 * How an import behaves.
 *
 * @property onInvalid What to do when an event is invalid. Defaults to [OnInvalidEvent.Reject]: nothing is committed.
 * @property waitForCommit Whether completing the import waits for the commit to finish. When `false`, completing
 *   returns once the commit has started, and progress tells when it ends.
 * @property maxChunksInFlight The most chunks sent to the kernel at the same time. Reading the source pauses while
 *   this many are unacknowledged.
 * @property maxEventsPerChunk An upper bound on the events in a chunk, below the kernel's own limit; `null` uses
 *   the kernel's limit.
 * @property importId The identifier to begin the import under; `null` lets the kernel choose one.
 * @property maxChunkAttempts How many times a chunk is sent when the connection fails transiently. Sending a chunk
 *   again is safe: the kernel stages a chunk under its index once.
 */
data class ImportOptions @JvmOverloads constructor(
    val onInvalid: OnInvalidEvent = OnInvalidEvent.Reject,
    val waitForCommit: Boolean = true,
    val maxChunksInFlight: Int = DEFAULT_MAX_CHUNKS_IN_FLIGHT,
    val maxEventsPerChunk: Int? = null,
    val importId: ImportId? = null,
    val maxChunkAttempts: Int = DEFAULT_MAX_CHUNK_ATTEMPTS,
) {
    companion object {
        /** The default number of chunks in flight. */
        const val DEFAULT_MAX_CHUNKS_IN_FLIGHT: Int = 4

        /** The default number of attempts to send a chunk. */
        const val DEFAULT_MAX_CHUNK_ATTEMPTS: Int = 5

        /** The default options. */
        @JvmField
        val default: ImportOptions = ImportOptions()
    }
}
