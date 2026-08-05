// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventObservationState
import io.cratis.chronicle.events.EventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
data class BookReturned(val title: String)

@EventType
data class BookBorrowed(val title: String)

@EventType
data class MemberJoined(val name: String)

class ReactorHandlersTests {

    private val live = EventObservationState(EventObservationState.INITIAL)
    private val replaying = EventObservationState(EventObservationState.REPLAY)

    class PlainReactor {
        var handled = 0
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) {
            handled++
        }
    }

    class OnceOnlyMethodReactor {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit

        @OnceOnly
        fun memberJoined(@Suppress("UNUSED_PARAMETER") event: MemberJoined) = Unit
    }

    class ReplayHandlerReactor {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit

        @Replay
        fun bookReturnedDuringReplay(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
    }

    class ReplayOnlyReactor {
        @Replay
        fun bookBorrowed(@Suppress("UNUSED_PARAMETER") event: BookBorrowed) = Unit
    }

    class OnceOnlyReplayHandlerReactor {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit

        @Replay
        @OnceOnly
        fun bookReturnedDuringReplay(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
    }

    class ContextTakingReactor {
        fun bookReturned(
            @Suppress("UNUSED_PARAMETER") event: BookReturned,
            @Suppress("UNUSED_PARAMETER") context: EventContext
        ) = Unit
    }

    @Test
    fun `handler is resolved for a live event`() {
        val handlers = ReactorHandlers.from(PlainReactor::class)
        val resolution = handlers.resolve("BookReturned", live)
        val invoke = assertInstanceOf(ReactorHandlerResolution.Invoke::class.java, resolution)
        assertEquals("bookReturned", invoke.handler.function.name)
    }

    @Test
    fun `unknown event type is not handled`() {
        val handlers = ReactorHandlers.from(PlainReactor::class)
        assertEquals(ReactorHandlerResolution.NotHandled, handlers.resolve("SomethingElse", live))
    }

    @Test
    fun `plain handler still runs during replay`() {
        val handlers = ReactorHandlers.from(PlainReactor::class)
        assertInstanceOf(ReactorHandlerResolution.Invoke::class.java, handlers.resolve("BookReturned", replaying))
    }

    @Test
    fun `once only method runs when the event is live`() {
        val handlers = ReactorHandlers.from(OnceOnlyMethodReactor::class)
        assertInstanceOf(ReactorHandlerResolution.Invoke::class.java, handlers.resolve("MemberJoined", live))
    }

    @Test
    fun `once only method is skipped during replay`() {
        val handlers = ReactorHandlers.from(OnceOnlyMethodReactor::class)
        assertEquals(ReactorHandlerResolution.SkippedForReplay, handlers.resolve("MemberJoined", replaying))
    }

    @Test
    fun `once only on one method leaves the others replaying`() {
        val handlers = ReactorHandlers.from(OnceOnlyMethodReactor::class)
        assertInstanceOf(ReactorHandlerResolution.Invoke::class.java, handlers.resolve("BookReturned", replaying))
    }

    @Test
    fun `replay handler takes over during replay`() {
        val handlers = ReactorHandlers.from(ReplayHandlerReactor::class)
        val resolution = handlers.resolve("BookReturned", replaying)
        val invoke = assertInstanceOf(ReactorHandlerResolution.Invoke::class.java, resolution)
        assertEquals("bookReturnedDuringReplay", invoke.handler.function.name)
    }

    @Test
    fun `replay handler does not run for a live event`() {
        val handlers = ReactorHandlers.from(ReplayHandlerReactor::class)
        val resolution = handlers.resolve("BookReturned", live)
        val invoke = assertInstanceOf(ReactorHandlerResolution.Invoke::class.java, resolution)
        assertEquals("bookReturned", invoke.handler.function.name)
    }

    @Test
    fun `event type handled only by a replay handler is still subscribed to`() {
        val handlers = ReactorHandlers.from(ReplayOnlyReactor::class)
        assertTrue(handlers.eventTypes.containsKey("BookBorrowed"))
    }

    @Test
    fun `once only replay handler is skipped during replay`() {
        val handlers = ReactorHandlers.from(OnceOnlyReplayHandlerReactor::class)
        assertEquals(ReactorHandlerResolution.SkippedForReplay, handlers.resolve("BookReturned", replaying))
    }

    @Test
    fun `handler taking an event context is recognized`() {
        val handlers = ReactorHandlers.from(ContextTakingReactor::class)
        assertInstanceOf(ReactorHandlerResolution.Invoke::class.java, handlers.resolve("BookReturned", live))
    }

    @Test
    fun `methods that are not handlers are ignored`() {
        val handlers = ReactorHandlers.from(PlainReactor::class)
        assertEquals(1, handlers.eventTypes.size)
    }
}
