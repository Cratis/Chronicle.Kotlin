// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

/**
 * Represents the unique identifier of an event sequence.
 */
@JvmInline
value class EventSequenceId(val value: String) {
    companion object {
        /** The well-known identifier for the default event log sequence. */
        val eventLog: EventSequenceId = EventSequenceId("event-log")
    }

    override fun toString(): String = value
}
