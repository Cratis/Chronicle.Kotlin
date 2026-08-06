// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventObservationState
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeGeneration
import io.cratis.chronicle.events.EventTypeId
import io.cratis.chronicle.identity.Identity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * A handler that has to await something - an HTTP call, the event log - should be able to say so
 * with `suspend` rather than blocking the thread the observation runs on.
 */
class SuspendingHandlerTests {

    private val live = EventObservationState(EventObservationState.INITIAL)

    private fun contextFor(eventSourceId: String) = EventContext(
        sequenceNumber = 0,
        eventSourceId = eventSourceId,
        eventType = EventTypeDescriptor(EventTypeId("BookReturned"), EventTypeGeneration.first),
        occurred = Instant.EPOCH,
        correlationId = UUID.randomUUID(),
        causedBy = Identity.system
    )

    class SuspendingReactor {
        var handled = ""

        suspend fun bookReturned(event: BookReturned) {
            delay(1)
            handled = event.title
        }
    }

    class SuspendingContextTakingReactor {
        var handledIn = ""

        suspend fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned, context: EventContext) {
            delay(1)
            handledIn = context.eventSourceId
        }
    }

    @Reducer
    class SuspendingReducer {
        suspend fun borrowed(event: BookBorrowed, state: BookState?): BookState {
            delay(1)
            return (state ?: BookState("", false)).copy(title = event.title, borrowed = true)
        }
    }

    @Test
    fun `a suspending reactor handler is discovered`() {
        val handlers = ReactorHandlers.from(SuspendingReactor::class)
        assertEquals(setOf("BookReturned"), handlers.eventTypes.keys)
    }

    @Test
    fun `a suspending reactor handler is awaited`() = runBlocking {
        val handlers = ReactorHandlers.from(SuspendingReactor::class)
        val resolution = handlers.resolve("BookReturned", live)
        val reactor = SuspendingReactor()

        assertInstanceOf(ReactorHandlerResolution.Invoke::class.java, resolution)
        (resolution as ReactorHandlerResolution.Invoke).handler.invoke(reactor, BookReturned("Dune"))

        assertEquals("Dune", reactor.handled)
    }

    @Test
    fun `a suspending reactor handler still receives its context`() = runBlocking {
        val handlers = ReactorHandlers.from(SuspendingContextTakingReactor::class)
        val resolution = handlers.resolve("BookReturned", live)
        val reactor = SuspendingContextTakingReactor()
        val context = contextFor("book-1")

        (resolution as ReactorHandlerResolution.Invoke).handler.invoke(reactor, BookReturned("Dune"), context)

        assertEquals("book-1", reactor.handledIn)
    }

    @Test
    fun `a suspending reducer handler is awaited and its state returned`() = runBlocking {
        val registration = ReducerRegistration.from(SuspendingReducer::class)
        val handler = registration.handlers.getValue("BookBorrowed")

        val state = handler.invoke(SuspendingReducer(), BookBorrowed("Dune"), null)

        assertEquals(BookState("Dune", true), state)
    }

    @Test
    fun `a plain handler goes through the same path unchanged`() = runBlocking {
        val handlers = ReactorHandlers.from(ReactorHandlersTests.PlainReactor::class)
        val resolution = handlers.resolve("BookReturned", live)
        val reactor = ReactorHandlersTests.PlainReactor()

        (resolution as ReactorHandlerResolution.Invoke).handler.invoke(reactor, BookReturned("Dune"))

        assertEquals(1, reactor.handled)
    }
}
