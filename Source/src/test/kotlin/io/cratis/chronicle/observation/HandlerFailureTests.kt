// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.InvocationTargetException

/**
 * What the kernel is told when a handler fails is what someone eventually reads off a stuck
 * partition, so it has to be the handler's own message - not the one reflection wrapped it in.
 */
class HandlerFailureTests {

    @Test
    fun `a reflection wrapper is unwrapped to what the handler actually raised`() {
        val actual = IllegalStateException("mail server refused the connection")
        val wrapped = InvocationTargetException(actual)

        assertSame(actual, wrapped.unwrapReflectionFailure())
    }

    @Test
    fun `a failure that was not wrapped is left alone`() {
        val actual = IllegalStateException("mail server refused the connection")
        assertSame(actual, actual.unwrapReflectionFailure())
    }

    @Test
    fun `the reported message is the handler's own`() {
        val wrapped = InvocationTargetException(IllegalStateException("mail server refused the connection"))

        assertEquals("mail server refused the connection", wrapped.messageFor("employeeHired"))
    }

    @Test
    fun `a failure with no message of its own names its type and the handler`() {
        val wrapped = InvocationTargetException(NullPointerException())

        assertEquals("NullPointerException in employeeHired", wrapped.messageFor("employeeHired"))
    }

    @Test
    fun `a blank message is treated as no message at all`() {
        assertEquals("IllegalStateException in employeeHired", IllegalStateException("  ").messageFor("employeeHired"))
    }

    @Test
    fun `an outcome records the handler's message and its stack trace`() {
        val outcome = ReactorObservationOutcome()

        outcome.failed(
            InvocationTargetException(IllegalStateException("mail server refused the connection")),
            "employeeHired"
        )

        assertEquals(listOf("mail server refused the connection"), outcome.exceptions)
        assertTrue(outcome.stackTrace.contains("IllegalStateException"), outcome.stackTrace)
        assertTrue(!outcome.isSuccess)
    }
}
