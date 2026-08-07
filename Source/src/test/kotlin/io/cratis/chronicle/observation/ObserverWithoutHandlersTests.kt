// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.readModels.ReadModelReactorHandlers
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * An observer with no handlers subscribes to nothing, so the kernel has nothing to deliver to it and
 * it silently never runs. Registration is the only moment the cause is still visible.
 */
class ObserverWithoutHandlersTests {

    @Reactor
    class ReactorWithNothingToHandle {
        // Shaped like a handler, but BookReturned is what carries @EventType - a String does not.
        fun bookReturned(@Suppress("UNUSED_PARAMETER") title: String) = Unit
    }

    @Reducer
    class ReducerWithNothingToHandle {
        fun borrowed(@Suppress("UNUSED_PARAMETER") title: String) = BookState("", false)
    }

    class ReadModelReactorWithNothingToHandle {
        fun notAChangeName(@Suppress("UNUSED_PARAMETER") state: BookState) = Unit
    }

    @Test
    fun `a reactor with no handlers is rejected`() {
        val error = assertThrows(ObserverHasNoHandlers::class.java) {
            ReactorHandlers.from(ReactorWithNothingToHandle::class)
        }
        assertTrue(error.message!!.contains("ReactorWithNothingToHandle"))
        assertTrue(error.message!!.contains("@EventType"))
    }

    @Test
    fun `a reducer with no handlers is rejected`() {
        val error = assertThrows(ObserverHasNoHandlers::class.java) {
            ReducerRegistration.from(ReducerWithNothingToHandle::class)
        }
        assertTrue(error.message!!.contains("ReducerWithNothingToHandle"))
        assertTrue(error.message!!.contains("@EventType"))
    }

    @Test
    fun `a read model reactor with no handlers is rejected`() {
        val error = assertThrows(ObserverHasNoHandlers::class.java) {
            ReadModelReactorHandlers.from(ReadModelReactorWithNothingToHandle::class)
        }
        assertTrue(error.message!!.contains("ReadModelReactorWithNothingToHandle"))
    }

    @Test
    fun `the message explains what a handler has to look like`() {
        val error = assertThrows(ObserverHasNoHandlers::class.java) {
            ReactorHandlers.from(ReactorWithNothingToHandle::class)
        }
        assertTrue(error.message!!.contains("observe nothing"))
        assertTrue(error.message!!.contains("first parameter"))
    }
}
