// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences.concurrency

import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.events.EventTypeDescriptor

/**
 * Represents a concurrency scope for an event sequence append operation.
 *
 * @property sequenceNumber The expected [EventSequenceNumber] the kernel validates the append against.
 * @property eventSourceId Whether to narrow the scope to the event source id the append targets.
 * @property eventStreamType Optional event stream type to narrow the scope to. If not set, it is not used.
 * @property eventStreamId Optional event stream id to narrow the scope to. If not set, it is not used.
 * @property eventSourceType Optional event source type to narrow the scope to. If not set, it is not used.
 * @property eventTypes Optional collection of event types to narrow the scope to. If empty, it is not used.
 * @property expectsNoMatchingEvent Whether the kernel should reject the append if any event matching
 *   the scope already exists. Setting this flag and a concrete [sequenceNumber] simultaneously is a
 *   programming error and is rejected at construction time: "no matching event" implies the scope has
 *   no events to compare a sequence number against.
 */
data class ConcurrencyScope(
    val sequenceNumber: EventSequenceNumber,
    val eventSourceId: Boolean = false,
    val eventStreamType: String? = null,
    val eventStreamId: String? = null,
    val eventSourceType: String? = null,
    val eventTypes: List<EventTypeDescriptor> = emptyList(),
    val expectsNoMatchingEvent: Boolean = false
) {
    init {
        require(!(expectsNoMatchingEvent && sequenceNumber.isActualValue)) {
            "expectsNoMatchingEvent and a concrete sequenceNumber are mutually exclusive: " +
                "'expects no matching event' means the scope has no events to validate a sequence " +
                "number against. Use EventSequenceNumber.unavailable or omit sequenceNumber."
        }
    }

    companion object {
        /** A concurrency scope that has not been specified yet. */
        val notSet: ConcurrencyScope = ConcurrencyScope(EventSequenceNumber.max)

        /** A concurrency scope that applies no constraints — the append is not concurrency-checked. */
        val none: ConcurrencyScope = ConcurrencyScope(EventSequenceNumber.unavailable)
    }

    /**
     * Whether the scope narrows an append without saying which sequence number it expects.
     *
     * A scope in this state is a programming error rather than a way to opt out — it is not [notSet], so
     * nothing resolves the expected sequence number for it, and it has no sequence number of its own for
     * the kernel to validate against, so the append is checked against nothing. Use [none] to append
     * without a check, or [notSet] to have the expected sequence number resolved for you.
     *
     * A scope with [expectsNoMatchingEvent] is never incomplete: the kernel has a well-defined check to
     * perform and no sequence number is needed for it.
     */
    val isIncomplete: Boolean get() = this != notSet && this != none && !sequenceNumber.isActualValue && !expectsNoMatchingEvent
}
