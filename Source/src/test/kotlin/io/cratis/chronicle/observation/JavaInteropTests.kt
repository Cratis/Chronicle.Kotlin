// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.events.EventObservationState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.reflect.full.findAnnotation

/**
 * Proves the observation annotations and handler conventions work for reactors and reducers written
 * in Java, not just Kotlin. Java has no notion of Kotlin's default arguments, so an annotation that
 * reads fine from Kotlin can still be unusable from Java - these specs pin that down.
 */
class JavaInteropTests {

    private val live = EventObservationState(EventObservationState.INITIAL)
    private val replaying = EventObservationState(EventObservationState.REPLAY)

    @Test
    fun `java reactor annotation is read`() {
        val annotation = JavaReactor::class.findAnnotation<Reactor>()!!
        assertEquals("java-reactor", annotation.id)
        assertEquals("outbox", annotation.eventSequence)
    }

    @Test
    fun `java reactor annotation defaults are applied`() {
        val annotation = JavaOnceOnlyReactor::class.findAnnotation<Reactor>()!!
        assertEquals("", annotation.id)
        assertEquals("", annotation.eventSequence)
    }

    @Test
    fun `java reactor handlers are discovered`() {
        val handlers = ReactorHandlers.from(JavaReactor::class)
        assertEquals(setOf("JavaBookAdded", "JavaBookRemoved"), handlers.eventTypes.keys)
    }

    @Test
    fun `java handler taking an event context is invoked`() {
        val handlers = ReactorHandlers.from(JavaReactor::class)
        val resolution = handlers.resolve("JavaBookAdded", live)
        val invoke = assertInstanceOf(ReactorHandlerResolution.Invoke::class.java, resolution)
        assertEquals("bookAdded", invoke.handler.function.name)
    }

    @Test
    fun `java replay handler takes over during replay`() {
        val handlers = ReactorHandlers.from(JavaReactor::class)
        val resolution = handlers.resolve("JavaBookAdded", replaying)
        val invoke = assertInstanceOf(ReactorHandlerResolution.Invoke::class.java, resolution)
        assertEquals("bookAddedDuringReplay", invoke.handler.function.name)
    }

    @Test
    fun `java once only method is skipped during replay`() {
        val handlers = ReactorHandlers.from(JavaReactor::class)
        assertEquals(ReactorHandlerResolution.SkippedForReplay, handlers.resolve("JavaBookRemoved", replaying))
    }

    @Test
    fun `java once only method runs when live`() {
        val handlers = ReactorHandlers.from(JavaReactor::class)
        assertInstanceOf(ReactorHandlerResolution.Invoke::class.java, handlers.resolve("JavaBookRemoved", live))
    }

    @Test
    fun `class level once only is readable on a java reactor`() {
        assertTrue(JavaOnceOnlyReactor::class.findAnnotation<OnceOnly>() != null)
        assertTrue(JavaReactor::class.findAnnotation<OnceOnly>() == null)
    }

    @Test
    fun `java standalone event sequence annotation is read`() {
        // JavaEventSequenceReactor writes @EventSequence("outbox") in Java's shorthand form, which
        // only compiles while the Kotlin parameter is named `value` - so this failing to build at
        // all is the real assertion here.
        assertEquals("outbox", JavaEventSequenceReactor::class.findAnnotation<EventSequence>()!!.value)
        assertEquals("outbox", ReactorRegistration.from(JavaEventSequenceReactor::class).eventSequenceId)
    }

    @Test
    fun `java standalone event sequence annotation wins over the parameter`() {
        assertEquals("from-standalone", ReducerRegistration.from(JavaEventSequenceReducer::class).eventSequenceId)
    }

    @Test
    fun `java reducer annotation is read`() {
        val registration = ReducerRegistration.from(JavaReducer::class)
        assertEquals("java-reducer", registration.id)
        assertEquals("outbox", registration.eventSequenceId)
        assertFalse(registration.isActive)
    }

    @Test
    fun `java reducer annotation defaults are applied`() {
        val registration = ReducerRegistration.from(JavaDefaultReducer::class)
        assertEquals("JavaDefaultReducer", registration.id)
        assertEquals(EventSequenceId.eventLog.value, registration.eventSequenceId)
        assertTrue(registration.isActive)
    }

    @Test
    fun `java reducer handlers are discovered`() {
        val registration = ReducerRegistration.from(JavaReducer::class)
        assertEquals(setOf("JavaBookAdded", "JavaBookRemoved"), registration.handlers.keys)
    }

    @Test
    fun `java reducer read model is inferred`() {
        val registration = ReducerRegistration.from(JavaReducer::class)
        assertEquals(JavaBookState::class, registration.readModelClass)
        assertEquals("JavaBookState", registration.readModelName)
    }

    @Test
    fun `java repeated tag annotations are read`() {
        // Kotlin's @Repeatable only reaches Java through a generated JVM container annotation, so
        // repeating @Tag from Java is worth pinning down rather than assuming.
        assertEquals(
            listOf("analytics", "reporting", "owned-by-platform"),
            ObserverFilters.tagsOf(JavaTaggedReactor::class)
        )
    }

    @Test
    fun `java repeated filter annotations are read`() {
        val filters = ObserverFilters.from(JavaTaggedReactor::class)
        assertEquals(listOf("critical", "production"), filters.filterTags)
        assertEquals("Patient", filters.eventSourceType)
        assertEquals("Onboarding", filters.eventStreamType)
    }

    @Test
    fun `java reducer handler taking an event context has four parameters`() {
        val registration = ReducerRegistration.from(JavaReducer::class)
        // Index 0 is the instance receiver, so the context-taking shape arrives as four.
        assertEquals(4, registration.handlers["JavaBookRemoved"]!!.function.parameters.size)
        assertEquals(3, registration.handlers["JavaBookAdded"]!!.function.parameters.size)
    }
}
