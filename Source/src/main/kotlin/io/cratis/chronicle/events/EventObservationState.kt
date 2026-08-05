// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events

/**
 * The state an event arrives in when it is observed.
 *
 * This is a set of flags rather than a single value - an event that is the first one of a replay
 * arrives as both [replay] and [headOfReplay]. Test for a specific flag with the `is*` properties
 * rather than comparing values directly.
 *
 * @property value The raw flags value as it arrives from the kernel.
 */
data class EventObservationState(val value: Int) {
    /** Whether no flags at all are set. */
    val isNone: Boolean get() = value == NONE

    /** Whether this is the first time the event is being observed. */
    val isInitial: Boolean get() = has(INITIAL)

    /** Whether this event is the first one of a replay. */
    val isHeadOfReplay: Boolean get() = has(HEAD_OF_REPLAY)

    /** Whether this event is arriving as part of a replay. */
    val isReplay: Boolean get() = has(REPLAY)

    /** Whether this event is the last one of a replay. */
    val isTailOfReplay: Boolean get() = has(TAIL_OF_REPLAY)

    /** Whether [flag] is set. */
    fun has(flag: Int): Boolean = (value and flag) == flag && flag != NONE

    companion object {
        /** No flags set. */
        const val NONE = 0

        /** The event is being observed for the first time. */
        const val INITIAL = 1

        /** The event is the first one of a replay. */
        const val HEAD_OF_REPLAY = 2

        /** The event is arriving as part of a replay. */
        const val REPLAY = 4

        /** The event is the last one of a replay. */
        const val TAIL_OF_REPLAY = 8

        /** An [EventObservationState] with no flags set. */
        @JvmField
        val none = EventObservationState(NONE)

        /** An [EventObservationState] representing a first-time observation. */
        @JvmField
        val initial = EventObservationState(INITIAL)

        /** An [EventObservationState] representing an event arriving during a replay. */
        @JvmField
        val replay = EventObservationState(REPLAY)
    }
}
