// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventObservationState
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeGeneration
import io.cratis.chronicle.events.EventTypeId
import io.cratis.chronicle.identity.Identity
import io.cratis.chronicle.readModels.IReadModelsService
import io.cratis.chronicle.readModels.ReadModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.KParameter

@ReadModel
data class BookAvailability(val title: String = "", val onLoan: Boolean = false)

/**
 * A reactor handler used to be limited to the event and its context. These pin what a handler may now
 * ask for, and that a parameter nothing can supply is caught at registration rather than on every
 * event.
 */
class ReactorMethodArgumentsTests {

    private val live = EventObservationState(EventObservationState.INITIAL)

    private val context = EventContext(
        sequenceNumber = 0,
        eventSourceId = "book-1",
        eventType = EventTypeDescriptor(EventTypeId("BookReturned"), EventTypeGeneration.first),
        occurred = Instant.EPOCH,
        correlationId = UUID.randomUUID(),
        causedBy = Identity.system
    )

    class ReadModelTakingReactor {
        var seen: BookAvailability? = null
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned, book: BookAvailability?) {
            seen = book
        }
    }

    class ContextAndReadModelReactor {
        fun bookReturned(
            @Suppress("UNUSED_PARAMETER") event: BookReturned,
            @Suppress("UNUSED_PARAMETER") context: EventContext,
            @Suppress("UNUSED_PARAMETER") book: BookAvailability?
        ) = Unit
    }

    class UnsupportableParameterReactor {
        fun bookReturned(
            @Suppress("UNUSED_PARAMETER") event: BookReturned,
            @Suppress("UNUSED_PARAMETER") librarian: String
        ) = Unit
    }

    /** A resolver an application might add for something the client knows nothing about. */
    class LibrarianArgument : IReactorMethodArgumentResolver {
        override fun canResolve(parameter: KParameter): Boolean = parameter.type.classifier == String::class
        override suspend fun resolve(parameter: KParameter, context: EventContext): Any = "on duty"
    }

    private fun argumentsResolvingReadModel(instance: BookAvailability?): ReactorMethodArguments {
        val readModels = mockk<IReadModelsService>()
        coEvery { readModels.getInstanceByKey(any<KClass<Any>>(), any()) } returns instance
        return ReactorMethodArguments(listOf(ReadModelArgument(readModels)))
    }

    @Test
    fun `a handler taking only the event needs nothing resolved`() = runBlocking {
        val handlers = ReactorHandlers.from(ReactorHandlersTests.PlainReactor::class)
        val handler = (handlers.resolve("BookReturned", live) as ReactorHandlerResolution.Invoke).handler

        assertTrue(ReactorMethodArguments.contextOnly.resolve(handler, context).isEmpty())
    }

    @Test
    fun `the context is supplied without any resolver`() = runBlocking {
        val handlers = ReactorHandlers.from(ReactorHandlersTests.ContextTakingReactor::class)
        val handler = (handlers.resolve("BookReturned", live) as ReactorHandlerResolution.Invoke).handler

        assertEquals(listOf(context), ReactorMethodArguments.contextOnly.resolve(handler, context))
    }

    @Test
    fun `a read model parameter is resolved for the event source the event arrived under`() = runBlocking {
        val book = BookAvailability("Dune", onLoan = false)
        val arguments = argumentsResolvingReadModel(book)
        val handlers = ReactorHandlers.from(ReadModelTakingReactor::class, arguments)
        val handler = (handlers.resolve("BookReturned", live) as ReactorHandlerResolution.Invoke).handler

        val reactor = ReadModelTakingReactor()
        handler.invoke(reactor, BookReturned("Dune"), *arguments.resolve(handler, context).toTypedArray())

        assertEquals(book, reactor.seen)
    }

    @Test
    fun `a read model nothing has been projected for resolves to null`() = runBlocking {
        val arguments = argumentsResolvingReadModel(null)
        val handlers = ReactorHandlers.from(ReadModelTakingReactor::class, arguments)
        val handler = (handlers.resolve("BookReturned", live) as ReactorHandlerResolution.Invoke).handler

        assertNull(arguments.resolve(handler, context).single())
    }

    @Test
    fun `the context and a read model can be asked for together`() = runBlocking {
        val book = BookAvailability("Dune", onLoan = true)
        val arguments = argumentsResolvingReadModel(book)
        val handlers = ReactorHandlers.from(ContextAndReadModelReactor::class, arguments)
        val handler = (handlers.resolve("BookReturned", live) as ReactorHandlerResolution.Invoke).handler

        assertEquals(listOf(context, book), arguments.resolve(handler, context))
    }

    @Test
    fun `a parameter nothing can supply is rejected at registration`() {
        val error = assertThrows(InvalidHandlerSignature::class.java) {
            ReactorHandlers.from(UnsupportableParameterReactor::class, argumentsResolvingReadModel(null))
        }

        assertTrue(error.message!!.contains("librarian"))
        assertTrue(error.message!!.contains("IReactorMethodArgumentResolver"))
    }

    @Test
    fun `an application resolver makes its own parameter supportable`() = runBlocking {
        val arguments = ReactorMethodArguments(listOf(LibrarianArgument()))
        val handlers = ReactorHandlers.from(UnsupportableParameterReactor::class, arguments)
        val handler = (handlers.resolve("BookReturned", live) as ReactorHandlerResolution.Invoke).handler

        assertEquals(listOf("on duty"), arguments.resolve(handler, context))
    }

    @Test
    fun `the first resolver that claims a parameter is the one asked`() = runBlocking {
        val readModels = mockk<IReadModelsService>()
        coEvery { readModels.getInstanceByKey(any<KClass<Any>>(), any()) } returns BookAvailability("Dune")

        // An application resolver comes first, so it takes over a parameter the built-in one would
        // otherwise have claimed.
        val overriding = object : IReactorMethodArgumentResolver {
            override fun canResolve(parameter: KParameter) = parameter.type.classifier == BookAvailability::class
            override suspend fun resolve(parameter: KParameter, context: EventContext) = BookAvailability("Overridden")
        }
        val arguments = ReactorMethodArguments(listOf(overriding, ReadModelArgument(readModels)))
        val handlers = ReactorHandlers.from(ReadModelTakingReactor::class, arguments)
        val handler = (handlers.resolve("BookReturned", live) as ReactorHandlerResolution.Invoke).handler

        assertEquals(listOf(BookAvailability("Overridden")), arguments.resolve(handler, context))
    }
}
