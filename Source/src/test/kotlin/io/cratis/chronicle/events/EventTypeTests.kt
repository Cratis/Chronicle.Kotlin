// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class EventTypeTests {

    @Test
    fun `parse returns event type with id only`() {
        val et = EventTypeDescriptor.parse("EmployeeHired")
        assertEquals("EmployeeHired", et.id.value)
        assertEquals(1, et.generation.value)
        assertFalse(et.tombstone)
    }

    @Test
    fun `parse returns event type with id and generation`() {
        val et = EventTypeDescriptor.parse("EmployeeHired+2")
        assertEquals("EmployeeHired", et.id.value)
        assertEquals(2, et.generation.value)
    }

    @Test
    fun `parse returns event type with tombstone flag`() {
        val et = EventTypeDescriptor.parse("EmployeeDeleted+1+true")
        assertEquals("EmployeeDeleted", et.id.value)
        assertTrue(et.tombstone)
    }

    @Test
    fun `unknown event type has all-zeros UUID`() {
        assertEquals("00000000-0000-0000-0000-000000000000", EventTypeDescriptor.unknown.id.value)
    }

    @Test
    fun `first generation is 1`() {
        assertEquals(1, EventTypeGeneration.first.value)
    }
}
