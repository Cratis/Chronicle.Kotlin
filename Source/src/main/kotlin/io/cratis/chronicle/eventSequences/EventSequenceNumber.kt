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

        /** The first sequence number in an event sequence. */
        val first: EventSequenceNumber = EventSequenceNumber(0)

        /**
         * The value used when a sequence number is unavailable, encoded as `-1L` so it round-trips
         * as `ulong.MaxValue` on the wire — this is what disables concurrency validation server-side.
         */
        val unavailable: EventSequenceNumber = EventSequenceNumber(-1L)

        /**
         * The maximum sequence number, encoded as `-3L` so it round-trips as `ulong.MaxValue - 2` on
         * the wire, matching the .NET client's `EventSequenceNumber.Max`.
         */
        val max: EventSequenceNumber = EventSequenceNumber(-3L)
    }

    /**
     * Whether this represents an actual sequence number, as opposed to a system sentinel such as
     * [unavailable] or [max].
     */
    val isActualValue: Boolean get() = this != unavailable && this != max

    /** Whether this sequence number is [unavailable]. */
    val isUnavailable: Boolean get() = this == unavailable

    override fun toString(): String = value.toString()
}
