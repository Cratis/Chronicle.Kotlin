// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EventObservationStateTests {

    @Test
    fun `none has no flags set`() {
        val state = EventObservationState(EventObservationState.NONE)
        assertTrue(state.isNone)
        assertFalse(state.isInitial)
        assertFalse(state.isReplay)
    }

    @Test
    fun `initial is not a replay`() {
        val state = EventObservationState(EventObservationState.INITIAL)
        assertTrue(state.isInitial)
        assertFalse(state.isReplay)
        assertFalse(state.isNone)
    }

    @Test
    fun `replay is recognized`() {
        val state = EventObservationState(EventObservationState.REPLAY)
        assertTrue(state.isReplay)
        assertFalse(state.isInitial)
    }

    @Test
    fun `head of replay is also a replay`() {
        val state = EventObservationState(
            EventObservationState.REPLAY or EventObservationState.HEAD_OF_REPLAY
        )
        assertTrue(state.isReplay)
        assertTrue(state.isHeadOfReplay)
        assertFalse(state.isTailOfReplay)
    }

    @Test
    fun `tail of replay is also a replay`() {
        val state = EventObservationState(
            EventObservationState.REPLAY or EventObservationState.TAIL_OF_REPLAY
        )
        assertTrue(state.isReplay)
        assertTrue(state.isTailOfReplay)
        assertFalse(state.isHeadOfReplay)
    }

    @Test
    fun `none flag is never considered set`() {
        assertFalse(EventObservationState(EventObservationState.REPLAY).has(EventObservationState.NONE))
    }
}
