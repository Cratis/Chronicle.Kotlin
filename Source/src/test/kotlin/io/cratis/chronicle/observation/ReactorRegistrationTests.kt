// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.eventSequences.EventSequenceId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ReactorRegistrationTests {

    class UnannotatedReactor {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
    }

    @Reactor
    class DefaultedReactor {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
    }

    @Reactor(id = "custom-id", eventSequence = "outbox")
    class ConfiguredReactor {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
    }

    @Reactor
    @OnceOnly
    class NonReplayableReactor {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
    }

    @Reactor
    class MethodLevelOnceOnlyReactor {
        @OnceOnly
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
    }

    @Test
    fun `id defaults to the class simple name`() {
        assertEquals("DefaultedReactor", ReactorRegistration.from(DefaultedReactor::class).id)
    }

    @Test
    fun `id defaults to the class simple name without the annotation`() {
        assertEquals("UnannotatedReactor", ReactorRegistration.from(UnannotatedReactor::class).id)
    }

    @Test
    fun `explicit id is used`() {
        assertEquals("custom-id", ReactorRegistration.from(ConfiguredReactor::class).id)
    }

    @Test
    fun `event sequence defaults to the event log`() {
        assertEquals(
            EventSequenceId.eventLog.value,
            ReactorRegistration.from(DefaultedReactor::class).eventSequenceId
        )
    }

    @Test
    fun `explicit event sequence is used`() {
        assertEquals("outbox", ReactorRegistration.from(ConfiguredReactor::class).eventSequenceId)
    }

    @Test
    fun `a reactor is replayable by default`() {
        assertTrue(ReactorRegistration.from(DefaultedReactor::class).isReplayable)
    }

    @Test
    fun `class level once only makes the reactor non-replayable`() {
        assertFalse(ReactorRegistration.from(NonReplayableReactor::class).isReplayable)
    }

    @Test
    fun `method level once only leaves the reactor replayable`() {
        // The whole reactor must still replay - only the marked handler is skipped, and that is
        // decided per event rather than at registration.
        assertTrue(ReactorRegistration.from(MethodLevelOnceOnlyReactor::class).isReplayable)
    }

    @Test
    fun `handlers are discovered`() {
        assertTrue(ReactorRegistration.from(DefaultedReactor::class).handlers.eventTypes.containsKey("BookReturned"))
    }

    @Test
    fun `java reactor registration is read`() {
        val registration = ReactorRegistration.from(JavaReactor::class)
        assertEquals("java-reactor", registration.id)
        assertEquals("outbox", registration.eventSequenceId)
        assertTrue(registration.isReplayable)
    }

    @Test
    fun `java class level once only makes the reactor non-replayable`() {
        assertFalse(ReactorRegistration.from(JavaOnceOnlyReactor::class).isReplayable)
    }
}
