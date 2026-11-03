// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import java.time.Instant

/**
 * What a batch method knows about the page of events it is called for.
 *
 * Only batch methods - those taking an [EventBatch] - can ask for it. A per-event handler that
 * declares a [BatchContext] parameter is refused at registration.
 *
 * @property observerId The identifier of the reactor or reducer.
 * @property eventStore The event store the events come from.
 * @property namespace The namespace the events come from.
 * @property eventSequenceId The event sequence the events come from.
 * @property origin Where the page came from: live, catch-up, replay or retry.
 * @property partitions The partitions - event source identifiers - in the page, in the order the kernel sent them.
 * @property eventCount The number of events in the page.
 * @property firstSequenceNumber The sequence number of the first event in the page, or -1 for an empty page.
 * @property lastSequenceNumber The sequence number of the last event in the page, or -1 for an empty page.
 * @property deadline When the kernel stops waiting for the result, or `null` when it waits without a deadline.
 */
class BatchContext @JvmOverloads constructor(
    val observerId: String,
    val eventStore: String,
    val namespace: String,
    val eventSequenceId: String,
    val origin: ObservationOrigin,
    val partitions: List<String> = emptyList(),
    val eventCount: Int = 0,
    val firstSequenceNumber: Long = NO_SEQUENCE_NUMBER,
    val lastSequenceNumber: Long = NO_SEQUENCE_NUMBER,
    val deadline: Instant? = null,
) {
    /** Whether the page is part of a replay. */
    val isReplay: Boolean get() = origin == ObservationOrigin.Replay

    /**
     * Whether the kernel accepts deferred partitions for the page. A kernel that does not deliver
     * pages does not; a deferral is then reported as a failure instead.
     */
    internal var canDefer: Boolean = true
        private set

    internal fun withoutDeferral(): BatchContext = also { canDefer = false }

    override fun toString(): String =
        "BatchContext(observerId=$observerId, origin=$origin, partitions=${partitions.size}, " +
            "eventCount=$eventCount, first=$firstSequenceNumber, last=$lastSequenceNumber)"

    companion object {
        /** The sequence number reported for an empty page. */
        const val NO_SEQUENCE_NUMBER: Long = -1L

        /** A context for a batch that carries no page information, such as a local fold of a passive reducer. */
        internal fun unknown(observerId: String, eventSequenceId: String): BatchContext =
            BatchContext(observerId, "", "", eventSequenceId, ObservationOrigin.Unknown).withoutDeferral()
    }
}
