// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

/**
 * Represents a position (sequence number) within an event sequence.
 */
@JvmInline
value class EventSequenceNumber(val value: Long) {
    companion object {
        /** The first sequence number in an event sequence. */
        val first: EventSequenceNumber = EventSequenceNumber(0)

        /**
         * The value used when a sequence number is unavailable, encoded as `-1L` so it round-trips
         * as `ulong.MaxValue` on the wire — this is what disables concurrency validation server-side.
         */
        val unavailable: EventSequenceNumber = EventSequenceNumber(-1L)

        /** The maximum sequence-number sentinel used by the 16.44.1 wire contract. */
        val max: EventSequenceNumber = EventSequenceNumber(-2L)

        /** Internal position before the first event. It must never be sent as an expected number. */
        internal val beforeFirst: EventSequenceNumber = EventSequenceNumber(-3L)
    }

    /**
     * Whether this represents an actual sequence number, as opposed to a system sentinel such as
     * [unavailable], [max], or the internal before-first position.
     */
    val isActualValue: Boolean get() = value >= 0

    /** Whether this sequence number is [unavailable]. */
    val isUnavailable: Boolean get() = this == unavailable

    override fun toString(): String = value.toString()
}
