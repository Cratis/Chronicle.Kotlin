// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

/**
 * Represents a position (sequence number) within an event sequence.
 */
@JvmInline
value class EventSequenceNumber(val value: Long) {
    companion object {
        /** Sentinel value indicating an unset sequence number ([Long.MAX_VALUE]). */
        val unset: EventSequenceNumber = EventSequenceNumber(Long.MAX_VALUE)
    }

    override fun toString(): String = value.toString()
}
