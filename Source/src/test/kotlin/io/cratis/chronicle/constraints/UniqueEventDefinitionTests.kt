// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private data class PersonEvent(val firstName: String, val lastName: String)

class UniqueEventDefinitionTests {
    @Test
    fun `a property named twice is rejected with a message naming it`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            UniqueEventDefinition(PersonEvent::class, listOf("firstName", "lastName", "firstName"))
        }

        assertTrue(error.message!!.contains("firstName"))
    }

    @Test
    fun `no properties is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { UniqueEventDefinition(PersonEvent::class, emptyList()) }
    }

    @Test
    fun `changing the list passed in after construction does not change the definition`() {
        val names = mutableListOf("firstName")
        val definition = UniqueEventDefinition(PersonEvent::class, names)

        names.add("lastName")
        names[0] = "other"

        assertEquals(listOf("firstName"), definition.properties)
    }

    @Test
    fun `equality and hash code follow event class and ordered properties`() {
        val first = UniqueEventDefinition(PersonEvent::class, listOf("firstName", "lastName"))

        assertEquals(first, UniqueEventDefinition(PersonEvent::class, listOf("firstName", "lastName")))
        assertEquals(first.hashCode(), UniqueEventDefinition(PersonEvent::class, listOf("firstName", "lastName")).hashCode())
        assertNotEquals(first, UniqueEventDefinition(PersonEvent::class, listOf("lastName", "firstName")))
    }

    @Test
    fun `changing the definitions list passed to a unique entry after construction does not change the entry`() {
        val definitions = mutableListOf(UniqueEventDefinition(PersonEvent::class, listOf("firstName")))
        val entry = ConstraintBuilderEntry.UniqueEntry(definitions, ignoreCasing = false, message = "")

        definitions.clear()

        assertEquals(1, entry.eventDefinitions.size)
    }
}
