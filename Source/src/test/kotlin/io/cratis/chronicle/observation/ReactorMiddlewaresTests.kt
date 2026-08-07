// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeGeneration
import io.cratis.chronicle.events.EventTypeId
import io.cratis.chronicle.identity.Identity
import io.cratis.chronicle.java.BlockingReactorMiddleware
import io.cratis.chronicle.java.asReactorMiddleware
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * Middlewares wrap handler invocation so cross-cutting concerns stay out of reactor code. What has to
 * hold: they nest rather than queue, they unwind even when the handler throws, and a middleware that
 * never ran on the way in is not unwound on the way out.
 */
class ReactorMiddlewaresTests {

    private val context = EventContext(
        sequenceNumber = 0,
        eventSourceId = "book-1",
        eventType = EventTypeDescriptor(EventTypeId("BookReturned"), EventTypeGeneration.first),
        occurred = Instant.EPOCH,
        correlationId = UUID.randomUUID(),
        causedBy = Identity.system
    )

    private val event = BookReturned("Dune")

    private class Recording(private val name: String, private val log: MutableList<String>) : IReactorMiddleware {
        override suspend fun beforeInvoke(context: EventContext, event: Any) {
            delay(1)
            log.add("$name.before")
        }

        override suspend fun afterInvoke(context: EventContext, event: Any) {
            log.add("$name.after")
        }
    }

    private class FailingOnTheWayIn(private val log: MutableList<String>) : IReactorMiddleware {
        override suspend fun beforeInvoke(context: EventContext, event: Any) =
            throw IllegalStateException("cannot start")

        override suspend fun afterInvoke(context: EventContext, event: Any) {
            log.add("failing.after")
        }
    }

    private class BlockingRecording(private val log: MutableList<String>) : BlockingReactorMiddleware {
        override fun beforeInvoke(context: EventContext, event: Any) {
            log.add("java.before")
        }

        override fun afterInvoke(context: EventContext, event: Any) {
            log.add("java.after")
        }
    }

    @Test
    fun `no middlewares invokes the handler directly`() = runBlocking {
        val result = ReactorMiddlewares.none.invoke(context, event) { "handled" }
        assertEquals("handled", result)
    }

    @Test
    fun `middlewares nest around the handler rather than queueing`() = runBlocking {
        val log = mutableListOf<String>()
        val middlewares = ReactorMiddlewares(listOf(Recording("outer", log), Recording("inner", log)))

        middlewares.invoke(context, event) { log.add("handler") }

        assertEquals(
            listOf("outer.before", "inner.before", "handler", "inner.after", "outer.after"),
            log
        )
    }

    @Test
    fun `what the handler returned is what comes back`() = runBlocking {
        val middlewares = ReactorMiddlewares(listOf(Recording("outer", mutableListOf())))
        assertEquals("side effect", middlewares.invoke(context, event) { "side effect" })
    }

    @Test
    fun `a handler that throws still unwinds every middleware`() {
        val log = mutableListOf<String>()
        val middlewares = ReactorMiddlewares(listOf(Recording("outer", log), Recording("inner", log)))

        assertThrows(IllegalStateException::class.java) {
            runBlocking { middlewares.invoke(context, event) { throw IllegalStateException("handler failed") } }
        }

        assertEquals(listOf("outer.before", "inner.before", "inner.after", "outer.after"), log)
    }

    @Test
    fun `a middleware that throws on the way in is not unwound, and neither is the handler run`() {
        val log = mutableListOf<String>()
        val middlewares = ReactorMiddlewares(
            listOf(Recording("outer", log), FailingOnTheWayIn(log), Recording("never", log))
        )

        assertThrows(IllegalStateException::class.java) {
            runBlocking { middlewares.invoke(context, event) { log.add("handler") } }
        }

        // The failing one never opened anything, nor did the one behind it, so only 'outer' unwinds.
        assertEquals(listOf("outer.before", "outer.after"), log)
    }

    @Test
    fun `a java middleware takes part in the chain like any other`() = runBlocking {
        val log = mutableListOf<String>()
        val middlewares = ReactorMiddlewares(
            listOf(Recording("kotlin", log), BlockingRecording(log).asReactorMiddleware())
        )

        middlewares.invoke(context, event) { log.add("handler") }

        assertEquals(
            listOf("kotlin.before", "java.before", "handler", "java.after", "kotlin.after"),
            log
        )
    }
}
