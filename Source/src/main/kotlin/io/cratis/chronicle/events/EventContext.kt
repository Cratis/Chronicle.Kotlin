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
 * @property observationState The [EventObservationState] this event is being observed in.
 *   Use this to tell a live event from one arriving during a replay.
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
    val observationState: EventObservationState = EventObservationState.none
)
