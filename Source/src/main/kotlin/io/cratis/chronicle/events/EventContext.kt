// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events

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
 */
data class EventContext(
    val sequenceNumber: Long,
    val eventSourceId: String,
    val eventType: EventTypeDescriptor,
    val occurred: Instant,
    val correlationId: UUID,
    val causedBy: Identity
)
