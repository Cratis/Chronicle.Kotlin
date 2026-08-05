// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private data class EmployeeEmailSet(val email: String, val name: String)

class ConstraintBuilderTests {

    @Test
    fun `on resolves the property actually passed to it`() {
        val builder = UniqueConstraintBuilder()
        builder.on(EmployeeEmailSet::class, EmployeeEmailSet::email)

        assertEquals("email", builder.build().propertyName)
    }

    @Test
    fun `on with a different property produces a constraint keyed on that property, not the first declared field`() {
        val emailBuilder = UniqueConstraintBuilder()
        emailBuilder.on(EmployeeEmailSet::class, EmployeeEmailSet::email)

        val nameBuilder = UniqueConstraintBuilder()
        nameBuilder.on(EmployeeEmailSet::class, EmployeeEmailSet::name)

        val emailEntry = emailBuilder.build()
        val nameEntry = nameBuilder.build()

        assertEquals("email", emailEntry.propertyName)
        assertEquals("name", nameEntry.propertyName)
        assertNotEquals(emailEntry.propertyName, nameEntry.propertyName)
    }

    @Test
    fun `unique DSL resolves the configured property through the fluent builder`() {
        val builder = ConstraintBuilder()
        builder.unique { it.on(EmployeeEmailSet::class, EmployeeEmailSet::name).ignoreCasing() }

        val entry = builder.build().single() as ConstraintBuilderEntry.UniqueEntry

        assertEquals("name", entry.propertyName)
        assertEquals(EmployeeEmailSet::class, entry.eventClass)
        assertTrue(entry.ignoreCasing)
    }

    @Test
    fun `onWithPropertyName resolves an explicit property name, for Java callers that cannot supply a KProperty1`() {
        val builder = UniqueConstraintBuilder()
        builder.onWithPropertyName(EmployeeEmailSet::class, "name")

        assertEquals("name", builder.build().propertyName)
    }
}
