// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.events.EventContext
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A handler the client cannot invoke must be rejected at registration. Left unchecked it registers
 * fine and then fails on every event, which is a far harder symptom to trace back.
 */
class HandlerSignatureValidationTests {

    class ReactorWithExtraParameter {
        fun bookReturned(
            @Suppress("UNUSED_PARAMETER") event: BookReturned,
            @Suppress("UNUSED_PARAMETER") context: EventContext,
            @Suppress("UNUSED_PARAMETER") extra: String
        ) = Unit
    }

    class ReactorWithWrongSecondParameter {
        fun bookReturned(
            @Suppress("UNUSED_PARAMETER") event: BookReturned,
            @Suppress("UNUSED_PARAMETER") notAContext: String
        ) = Unit
    }

    class SuspendingReactor {
        @Suppress("RedundantSuspendModifier")
        suspend fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
    }

    @Reducer
    class ReducerTakingContextInsteadOfState {
        fun borrowed(
            @Suppress("UNUSED_PARAMETER") event: BookBorrowed,
            @Suppress("UNUSED_PARAMETER") context: EventContext
        ) = BookState("", false)
    }

    @Reducer
    class ReducerWithWrongThirdParameter {
        fun borrowed(
            @Suppress("UNUSED_PARAMETER") event: BookBorrowed,
            @Suppress("UNUSED_PARAMETER") state: BookState?,
            @Suppress("UNUSED_PARAMETER") notAContext: String
        ) = BookState("", false)
    }

    @Reducer
    class SuspendingReducer {
        @Suppress("RedundantSuspendModifier")
        suspend fun borrowed(
            @Suppress("UNUSED_PARAMETER") event: BookBorrowed,
            @Suppress("UNUSED_PARAMETER") state: BookState?
        ) = BookState("", false)
    }

    @Test
    fun `reactor handler with too many parameters is rejected`() {
        assertThrows(InvalidHandlerSignature::class.java) {
            ReactorHandlers.from(ReactorWithExtraParameter::class)
        }
    }

    @Test
    fun `reactor handler whose second parameter is not a context is rejected`() {
        val error = assertThrows(InvalidHandlerSignature::class.java) {
            ReactorHandlers.from(ReactorWithWrongSecondParameter::class)
        }
        assertTrue(error.message!!.contains("EventContext"))
    }

    @Test
    fun `suspending reactor handler is rejected with a dedicated message`() {
        val error = assertThrows(InvalidHandlerSignature::class.java) {
            ReactorHandlers.from(SuspendingReactor::class)
        }
        assertTrue(error.message!!.contains("suspending"))
    }

    @Test
    fun `reducer handler taking a context instead of state is rejected`() {
        val error = assertThrows(InvalidHandlerSignature::class.java) {
            ReducerRegistration.from(ReducerTakingContextInsteadOfState::class)
        }
        assertTrue(error.message!!.contains("state so far"))
    }

    @Test
    fun `reducer handler whose third parameter is not a context is rejected`() {
        val error = assertThrows(InvalidHandlerSignature::class.java) {
            ReducerRegistration.from(ReducerWithWrongThirdParameter::class)
        }
        assertTrue(error.message!!.contains("EventContext"))
    }

    @Test
    fun `suspending reducer handler is rejected with a dedicated message`() {
        val error = assertThrows(InvalidHandlerSignature::class.java) {
            ReducerRegistration.from(SuspendingReducer::class)
        }
        assertTrue(error.message!!.contains("suspending"))
    }

    @Test
    fun `the error names the observer and the method`() {
        val error = assertThrows(InvalidHandlerSignature::class.java) {
            ReactorHandlers.from(ReactorWithWrongSecondParameter::class)
        }
        assertTrue(error.message!!.contains("ReactorWithWrongSecondParameter"))
        assertTrue(error.message!!.contains("bookReturned"))
    }

    @Test
    fun `valid reactor shapes are accepted`() {
        assertDoesNotThrow { ReactorHandlers.from(ReactorHandlersTests.PlainReactor::class) }
        assertDoesNotThrow { ReactorHandlers.from(ReactorHandlersTests.ContextTakingReactor::class) }
    }

    @Test
    fun `valid reducer shapes are accepted`() {
        assertDoesNotThrow { ReducerRegistration.from(ReducerRegistrationTests.DefaultedReducer::class) }
        assertDoesNotThrow { ReducerRegistration.from(ReducerRegistrationTests.ContextTakingReducer::class) }
    }

    @Test
    fun `java handler shapes are accepted`() {
        assertDoesNotThrow { ReactorHandlers.from(JavaReactor::class) }
        assertDoesNotThrow { ReducerRegistration.from(JavaReducer::class) }
    }
}
