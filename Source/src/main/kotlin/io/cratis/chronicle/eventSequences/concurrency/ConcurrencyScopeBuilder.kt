// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences.concurrency

import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.events.EventTypeDescriptor

/**
 * Builder for creating a [ConcurrencyScope] for an event sequence append operation.
 */
class ConcurrencyScopeBuilder {
    private var sequenceNumber: EventSequenceNumber = EventSequenceNumber.unavailable
    private var eventSourceId: Boolean = false
    private var eventStreamType: String? = null
    private var eventStreamId: String? = null
    private var eventSourceType: String? = null
    private val eventTypes = mutableListOf<EventTypeDescriptor>()

    /** Sets the expected [EventSequenceNumber] for the concurrency scope. */
    fun withSequenceNumber(sequenceNumber: EventSequenceNumber) = apply { this.sequenceNumber = sequenceNumber }

    /** Narrows the concurrency scope to the event source id the append targets. */
    fun withEventSourceId() = apply { this.eventSourceId = true }

    /** Narrows the concurrency scope to the given event stream type. */
    fun withEventStreamType(eventStreamType: String) = apply { this.eventStreamType = eventStreamType }

    /** Narrows the concurrency scope to the given event stream id. */
    fun withEventStreamId(eventStreamId: String) = apply { this.eventStreamId = eventStreamId }

    /** Narrows the concurrency scope to the given event source type. */
    fun withEventSourceType(eventSourceType: String) = apply { this.eventSourceType = eventSourceType }

    /** Adds an [EventTypeDescriptor] to narrow the concurrency scope to. */
    fun withEventType(eventType: EventTypeDescriptor) = apply { eventTypes.add(eventType) }

    /** Adds a collection of [EventTypeDescriptor]s to narrow the concurrency scope to. */
    fun withEventTypes(eventTypes: Collection<EventTypeDescriptor>) = apply { this.eventTypes.addAll(eventTypes) }

    /** Builds the [ConcurrencyScope] with the configured properties. */
    fun build(): ConcurrencyScope = ConcurrencyScope(
        sequenceNumber = sequenceNumber,
        eventSourceId = eventSourceId,
        eventStreamType = eventStreamType,
        eventStreamId = eventStreamId,
        eventSourceType = eventSourceType,
        eventTypes = eventTypes.distinct()
    )
}
