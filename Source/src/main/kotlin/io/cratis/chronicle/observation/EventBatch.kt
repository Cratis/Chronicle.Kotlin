// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.events.EventContext
import java.time.Duration

/**
 * The events a batch method is called with, in sequence order, and the way it reports partitions it
 * could not handle.
 *
 * Returning normally means every partition in the batch succeeded. Throwing fails every partition in
 * the batch at its first event in the batch. [fail] and [defer] report a single partition, so the
 * rest of the batch still succeeds.
 *
 * A partition is an event source: [EventContext.eventSourceId] identifies the partition of an event.
 *
 * @param events The events, in sequence order.
 */
class EventBatch(private val events: List<EventAndContext>) : List<EventAndContext> by events {
    private val reported = mutableListOf<BatchOutcome>()

    /** Whether the kernel accepts deferred partitions; a deferral is reported as a failure when it does not. */
    internal var canDefer: Boolean = true

    /** The partitions the batch holds events for, in the order their first event appears. */
    val partitions: List<String> get() = events.map { it.context.eventSourceId }.distinct()

    /** The outcomes reported for single partitions, in the order they were reported. */
    internal val outcomes: List<BatchOutcome> get() = synchronized(reported) { reported.toList() }

    /**
     * The partitions the batch method deferred, with how long each asked to wait before its events
     * are delivered again. When a partition is reported more than once, its earliest report wins.
     *
     * This is what a spec calling a batch method directly asserts on; Chronicle reads the same reports.
     */
    val deferred: Map<String, Duration>
        get() = earliestReports(BatchOutcome.Kind.Deferred).mapValues { it.value.retryAfter }

    /**
     * The partitions the batch method failed, with what failed each. A deferral the kernel cannot
     * accept is reported here, as a [DeferralNotSupportedByKernel]. When a partition is reported more
     * than once, its earliest report wins.
     */
    val failed: Map<String, Throwable>
        get() = earliestReports(BatchOutcome.Kind.Failed)
            .mapValues { it.value.error ?: PartitionFailedInBatch(it.key) }

    private fun earliestReports(kind: BatchOutcome.Kind): Map<String, BatchOutcome> =
        outcomes.groupBy { it.partition }
            .mapValues { (_, reports) -> reports.minBy { it.at } }
            .filterValues { it.kind == kind }

    /**
     * Fails [partition] from its first event in the batch. Its other events are delivered again once
     * the failed partition is recovered.
     *
     * @throws PartitionNotInBatch The batch holds no events for [partition].
     */
    fun fail(partition: String, error: Throwable) =
        report(partition, firstSequenceNumberOf(partition), BatchOutcome.Kind.Failed, error, Duration.ZERO)

    /**
     * Fails the partition of the event described by [context], from that event. The partition's
     * earlier events in the batch count as handled.
     *
     * @throws PartitionNotInBatch The batch holds no events for the event's partition.
     */
    fun fail(context: EventContext, error: Throwable) =
        report(context.eventSourceId, context.sequenceNumber, BatchOutcome.Kind.Failed, error, Duration.ZERO)

    /**
     * Defers [partition]: it is not failed, and is delivered again from its first event in the batch
     * after [retryAfter]. A deferral does not count towards quarantine.
     *
     * @throws PartitionNotInBatch The batch holds no events for [partition].
     */
    fun defer(partition: String, retryAfter: Duration) =
        report(partition, firstSequenceNumberOf(partition), BatchOutcome.Kind.Deferred, null, retryAfter)

    /**
     * Defers the partition of the event described by [context]: it is delivered again from that event
     * after [retryAfter], and its earlier events in the batch count as handled.
     *
     * @throws PartitionNotInBatch The batch holds no events for the event's partition.
     */
    fun defer(context: EventContext, retryAfter: Duration) =
        report(context.eventSourceId, context.sequenceNumber, BatchOutcome.Kind.Deferred, null, retryAfter)

    override fun toString(): String = "EventBatch(${events.size} events, ${partitions.size} partitions)"

    private fun firstSequenceNumberOf(partition: String): Long =
        events.firstOrNull { it.context.eventSourceId == partition }?.context?.sequenceNumber
            ?: throw PartitionNotInBatch(partition)

    private fun report(partition: String, at: Long, kind: BatchOutcome.Kind, error: Throwable?, retryAfter: Duration) {
        if (events.none { it.context.eventSourceId == partition }) throw PartitionNotInBatch(partition)

        val outcome = if (kind == BatchOutcome.Kind.Deferred && !canDefer) {
            BatchOutcome(partition, at, BatchOutcome.Kind.Failed, DeferralNotSupportedByKernel(partition), Duration.ZERO)
        } else {
            BatchOutcome(partition, at, kind, error, if (retryAfter.isNegative) Duration.ZERO else retryAfter)
        }
        synchronized(reported) { reported.add(outcome) }
    }
}
