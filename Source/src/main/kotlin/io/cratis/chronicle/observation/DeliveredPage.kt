// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.eventSequences.AppendedEvent
import java.time.Instant

/**
 * The events of one partition of a delivery, in sequence order.
 *
 * @property partition The partition, which is the event source the events belong to.
 * @property events The events.
 * @property initialState The stored read model as JSON, for reducers; empty when there is none.
 */
internal data class DeliveredPartition(
    val partition: String,
    val events: List<AppendedEvent>,
    val initialState: String = "",
) {
    /** The sequence numbers of the events, in order. */
    val sequenceNumbers: List<Long> get() = events.map { it.context.sequenceNumber }
}

/**
 * A delivery of events to an observer, independent of the transport that delivered it.
 *
 * A kernel that delivers pages hands over many partitions at once; an older kernel hands over one
 * partition at a time, which is simply a page of one that cannot be answered with a deferral.
 *
 * @property partitions The partitions, in the order the kernel sent them.
 * @property origin Where the page came from.
 * @property deliverWithinMilliseconds How long the kernel waits for the result; 0 when it waits without a deadline.
 * @property isPage Whether the kernel sent a page, and so accepts deferred partitions and expects one
 *   result per partition.
 */
internal class DeliveredPage(
    val partitions: List<DeliveredPartition>,
    val origin: ObservationOrigin = ObservationOrigin.Unknown,
    val deliverWithinMilliseconds: Long = 0L,
    val isPage: Boolean = true,
) {
    /** The [BatchContext] describing this page to a batch method of the observer it is delivered to. */
    fun contextFor(observerId: String, eventStore: String, namespace: String, eventSequenceId: String): BatchContext {
        val sequenceNumbers = partitions.flatMap { it.sequenceNumbers }
        val context = BatchContext(
            observerId = observerId,
            eventStore = eventStore,
            namespace = namespace,
            eventSequenceId = eventSequenceId,
            origin = origin,
            partitions = partitions.map { it.partition },
            eventCount = sequenceNumbers.size,
            firstSequenceNumber = sequenceNumbers.minOrNull() ?: BatchContext.NO_SEQUENCE_NUMBER,
            lastSequenceNumber = sequenceNumbers.maxOrNull() ?: BatchContext.NO_SEQUENCE_NUMBER,
            deadline = if (deliverWithinMilliseconds > 0) Instant.now().plusMillis(deliverWithinMilliseconds) else null,
        )
        return if (isPage) context else context.withoutDeferral()
    }
}
