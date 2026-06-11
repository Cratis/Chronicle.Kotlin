// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events

/**
 * Represents the unique string identifier for an event type.
 */
@JvmInline
value class EventTypeId(val value: String) {
    companion object {
        /** Represents an unknown event type identifier. */
        val unknown: EventTypeId = EventTypeId("00000000-0000-0000-0000-000000000000")
    }

    override fun toString(): String = value
}
