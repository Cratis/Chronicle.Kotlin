// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events

import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.identity.Identity
import java.time.Instant
import java.util.UUID

/**
 * Carries the metadata about an event that is available inside reactor and reducer handlers.
 *
 * @property sequenceNumber The position of this event in its event sequence.
 * @property eventSourceId The identifier of the event source that caused this event.
 * @property eventType The [EventTypeDescriptor] describing this event.
 * @property occurred When this event occurred.
 * @property correlationId The correlation identifier linking related operations.
 * @property causedBy The [Identity] that caused this event.
 * @property eventSourceType The type of the event source that caused this event.
 * @property eventStreamType The type of the event stream this event belongs to.
 * @property eventStreamId The identifier of the event stream this event belongs to.
 * @property eventStore The name of the event store this event belongs to.
 * @property namespace The namespace this event belongs to.
 * @property causation The chain describing what caused this event.
 * @property tags The tags associated with this event.
 * @property hash The hash of the event content.
 * @property subject The compliance subject acknowledged by the kernel, or empty when not supplied.
 * @property eventSource The name of the registered event source the event was appended through, or empty when
 *   it was not appended through a definition - including events stored before event sources existed.
 * @property observationState The [EventObservationState] this event is being observed in.
 *   Use this to tell a live event from one arriving during a replay.
 * @property compensationFor The sequence number of the event this event compensates for, or `null` when it is
 *   not a compensation. The wire format cannot say "absent", so a compensation of the event at sequence
 *   number `0` reads as `null` too; check the event type when that matters.
 */
data class EventContext(
    val sequenceNumber: Long,
    val eventSourceId: String,
    val eventType: EventTypeDescriptor,
    val occurred: Instant,
    val correlationId: UUID,
    val causedBy: Identity,
    val eventSourceType: String = "",
    val eventStreamType: String = "",
    val eventStreamId: String = "",
    val eventStore: String = "",
    val namespace: String = "",
    val causation: List<Causation> = emptyList(),
    val tags: List<String> = emptyList(),
    val hash: String = "",
    val observationState: EventObservationState = EventObservationState.none,
    val subject: String = "",
    val eventSource: String = "",
    val compensationFor: Long? = null
) {
    /** Whether this event is a compensation for another event. */
    val isCompensation: Boolean get() = compensationFor != null

    /**
     * Preserves the `copy` shape, and its default arguments, from before [compensationFor] existed;
     * [compensationFor] is carried over unchanged.
     */
    fun copy(
        sequenceNumber: Long = this.sequenceNumber,
        eventSourceId: String = this.eventSourceId,
        eventType: EventTypeDescriptor = this.eventType,
        occurred: Instant = this.occurred,
        correlationId: UUID = this.correlationId,
        causedBy: Identity = this.causedBy,
        eventSourceType: String = this.eventSourceType,
        eventStreamType: String = this.eventStreamType,
        eventStreamId: String = this.eventStreamId,
        eventStore: String = this.eventStore,
        namespace: String = this.namespace,
        causation: List<Causation> = this.causation,
        tags: List<String> = this.tags,
        hash: String = this.hash,
        observationState: EventObservationState = this.observationState,
        subject: String = this.subject,
        eventSource: String = this.eventSource
    ): EventContext = copy(
        sequenceNumber, eventSourceId, eventType, occurred, correlationId, causedBy, eventSourceType,
        eventStreamType, eventStreamId, eventStore, namespace, causation, tags, hash, observationState,
        subject, eventSource, compensationFor
    )

    /** Preserves the constructor shape, and its default arguments, from before [compensationFor] existed. */
    constructor(
        sequenceNumber: Long,
        eventSourceId: String,
        eventType: EventTypeDescriptor,
        occurred: Instant,
        correlationId: UUID,
        causedBy: Identity,
        eventSourceType: String = "",
        eventStreamType: String = "",
        eventStreamId: String = "",
        eventStore: String = "",
        namespace: String = "",
        causation: List<Causation> = emptyList(),
        tags: List<String> = emptyList(),
        hash: String = "",
        observationState: EventObservationState = EventObservationState.none,
        subject: String = "",
        eventSource: String = ""
    ) : this(
        sequenceNumber, eventSourceId, eventType, occurred, correlationId, causedBy, eventSourceType,
        eventStreamType, eventStreamId, eventStore, namespace, causation, tags, hash, observationState,
        subject, eventSource, null
    )

    /**
     * Preserves the `copy` shape, and its default arguments, from before [eventSource] existed;
     * [eventSource] and [compensationFor] are carried over unchanged.
     */
    fun copy(
        sequenceNumber: Long = this.sequenceNumber,
        eventSourceId: String = this.eventSourceId,
        eventType: EventTypeDescriptor = this.eventType,
        occurred: Instant = this.occurred,
        correlationId: UUID = this.correlationId,
        causedBy: Identity = this.causedBy,
        eventSourceType: String = this.eventSourceType,
        eventStreamType: String = this.eventStreamType,
        eventStreamId: String = this.eventStreamId,
        eventStore: String = this.eventStore,
        namespace: String = this.namespace,
        causation: List<Causation> = this.causation,
        tags: List<String> = this.tags,
        hash: String = this.hash,
        observationState: EventObservationState = this.observationState,
        subject: String = this.subject
    ): EventContext = copy(
        sequenceNumber, eventSourceId, eventType, occurred, correlationId, causedBy, eventSourceType,
        eventStreamType, eventStreamId, eventStore, namespace, causation, tags, hash, observationState,
        subject, eventSource, compensationFor
    )

    /** Preserves the constructor shape, and its default arguments, from before [eventSource] existed. */
    constructor(
        sequenceNumber: Long,
        eventSourceId: String,
        eventType: EventTypeDescriptor,
        occurred: Instant,
        correlationId: UUID,
        causedBy: Identity,
        eventSourceType: String = "",
        eventStreamType: String = "",
        eventStreamId: String = "",
        eventStore: String = "",
        namespace: String = "",
        causation: List<Causation> = emptyList(),
        tags: List<String> = emptyList(),
        hash: String = "",
        observationState: EventObservationState = EventObservationState.none,
        subject: String = ""
    ) : this(
        sequenceNumber, eventSourceId, eventType, occurred, correlationId, causedBy, eventSourceType,
        eventStreamType, eventStreamId, eventStore, namespace, causation, tags, hash, observationState,
        subject, "", null
    )
}

/**
 * Reads the wire `CompensationFor` value. The wire format cannot say "absent", so zero - what an event that
 * is not a compensation carries - reads as `null`.
 */
internal fun Long.toCompensatedSequenceNumber(): Long? = takeIf { it != 0L }
