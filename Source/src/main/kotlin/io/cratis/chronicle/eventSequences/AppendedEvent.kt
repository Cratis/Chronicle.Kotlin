// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import io.cratis.chronicle.events.EventContext

/**
 * Represents a single event read back from an event sequence.
 *
 * @property context The [EventContext] describing this event's position and metadata.
 * @property content The raw JSON content of the event, as stored. Deserialize it with the
 *   concrete event class matching [EventContext.eventType] to get a typed event object.
 */
data class AppendedEvent(
    val context: EventContext,
    val content: String
)
