// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.readModels.ReadModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@ReadModel
data class BookState(val title: String, val borrowed: Boolean)

@ReadModel(id = "books-with-a-name")
data class NamedBookState(val title: String)

class ReducerRegistrationTests {

    class UnannotatedReducer {
        fun borrowed(event: BookBorrowed, state: BookState?) = BookState(event.title, true)
    }

    @Reducer
    class DefaultedReducer {
        fun borrowed(event: BookBorrowed, state: BookState?) = BookState(event.title, true)
    }

    @Reducer(id = "custom-id", eventSequence = "outbox", isActive = false)
    class ConfiguredReducer {
        fun borrowed(event: BookBorrowed, state: BookState?) = BookState(event.title, true)
    }

    @Reducer
    @EventSequence("outbox")
    class StandaloneEventSequenceReducer {
        fun borrowed(event: BookBorrowed, state: BookState?) = BookState(event.title, true)
    }

    @Reducer(eventSequence = "from-parameter")
    @EventSequence("from-standalone")
    class DoublyConfiguredReducer {
        fun borrowed(event: BookBorrowed, state: BookState?) = BookState(event.title, true)
    }

    @Reducer
    class NamedReadModelReducer {
        fun borrowed(event: BookBorrowed, state: NamedBookState?) = NamedBookState(event.title)
    }

    @Reducer
    class ContextTakingReducer {
        fun borrowed(
            event: BookBorrowed,
            state: BookState?,
            @Suppress("UNUSED_PARAMETER") context: EventContext
        ) = BookState(event.title, true)
    }

    @Reducer
    class MultiHandlerReducer {
        fun borrowed(event: BookBorrowed, state: BookState?) = BookState(event.title, true)
        fun returned(event: BookReturned, state: BookState?) = BookState(event.title, false)
    }

    @Test
    fun `id defaults to the class simple name`() {
        assertEquals("DefaultedReducer", ReducerRegistration.from(DefaultedReducer::class).id)
    }

    @Test
    fun `id defaults to the class simple name without the annotation`() {
        assertEquals("UnannotatedReducer", ReducerRegistration.from(UnannotatedReducer::class).id)
    }

    @Test
    fun `explicit id is used`() {
        assertEquals("custom-id", ReducerRegistration.from(ConfiguredReducer::class).id)
    }

    @Test
    fun `event sequence defaults to the event log`() {
        assertEquals(EventSequenceId.eventLog.value, ReducerRegistration.from(DefaultedReducer::class).eventSequenceId)
    }

    @Test
    fun `explicit event sequence is used`() {
        assertEquals("outbox", ReducerRegistration.from(ConfiguredReducer::class).eventSequenceId)
    }

    @Test
    fun `standalone event sequence annotation is used`() {
        assertEquals("outbox", ReducerRegistration.from(StandaloneEventSequenceReducer::class).eventSequenceId)
    }

    @Test
    fun `standalone event sequence annotation wins over the parameter`() {
        // Matching the .NET client, where [EventSequence] on the class takes priority over the
        // event sequence declared on [Reducer].
        assertEquals("from-standalone", ReducerRegistration.from(DoublyConfiguredReducer::class).eventSequenceId)
    }

    @Test
    fun `reducer is active by default`() {
        assertTrue(ReducerRegistration.from(DefaultedReducer::class).isActive)
    }

    @Test
    fun `reducer is active by default without the annotation`() {
        assertTrue(ReducerRegistration.from(UnannotatedReducer::class).isActive)
    }

    @Test
    fun `reducer can be registered as passive`() {
        assertFalse(ReducerRegistration.from(ConfiguredReducer::class).isActive)
    }

    @Test
    fun `read model is inferred from the handler return type`() {
        assertEquals(BookState::class, ReducerRegistration.from(DefaultedReducer::class).readModelClass)
    }

    @Test
    fun `read model name defaults to the read model simple name`() {
        assertEquals("BookState", ReducerRegistration.from(DefaultedReducer::class).readModelName)
    }

    @Test
    fun `explicit read model id is used as the name`() {
        assertEquals("books-with-a-name", ReducerRegistration.from(NamedReadModelReducer::class).readModelName)
    }

    @Test
    fun `handlers are keyed by event type identifier`() {
        val registration = ReducerRegistration.from(MultiHandlerReducer::class)
        assertEquals(setOf("BookBorrowed", "BookReturned"), registration.handlers.keys)
    }

    @Test
    fun `handler taking an event context is recognized`() {
        val registration = ReducerRegistration.from(ContextTakingReducer::class)
        assertTrue(registration.handlers.containsKey("BookBorrowed"))
    }
}
