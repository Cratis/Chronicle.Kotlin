// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private data class EmployeeEmailSet(val email: String, val name: String)
private data class EmployeeMoved(val address: String)

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

    @Test
    fun `a constraint has no scope by default`() {
        val builder = ConstraintBuilder()
        builder.unique { it.on(EmployeeEmailSet::class, EmployeeEmailSet::email) }

        val entry = builder.build().single() as ConstraintBuilderEntry.UniqueEntry
        assertNull(entry.scope)
    }

    @Test
    fun `perEventSourceType scopes every constraint subsequently added through the same builder`() {
        val builder = ConstraintBuilder()
        builder.perEventSourceType()
        builder.unique { it.on(EmployeeEmailSet::class, EmployeeEmailSet::email) }
        builder.uniqueFor(EmployeeMoved::class)

        val entries = builder.build()
        entries.forEach { entry ->
            val scope = when (entry) {
                is ConstraintBuilderEntry.UniqueEntry -> entry.scope
                is ConstraintBuilderEntry.UniqueForEntry -> entry.scope
            }
            assertTrue(scope!!.perEventSourceType)
            assertFalse(scope.perEventStreamType)
            assertFalse(scope.perEventStreamId)
        }
    }

    @Test
    fun `perEventStreamType and perEventStreamId can be combined on the same constraint`() {
        val builder = ConstraintBuilder()
        builder.perEventStreamType().perEventStreamId()
        builder.unique { it.on(EmployeeEmailSet::class, EmployeeEmailSet::email) }

        val entry = builder.build().single() as ConstraintBuilderEntry.UniqueEntry
        assertFalse(entry.scope!!.perEventSourceType)
        assertTrue(entry.scope.perEventStreamType)
        assertTrue(entry.scope.perEventStreamId)
    }

    @Test
    fun `constraints added before a scoping call remain unscoped`() {
        val builder = ConstraintBuilder()
        builder.unique { it.on(EmployeeEmailSet::class, EmployeeEmailSet::email) }
        builder.perEventSourceType()
        builder.uniqueFor(EmployeeMoved::class)

        val entries = builder.build()
        val unscoped = entries.first() as ConstraintBuilderEntry.UniqueEntry
        val scoped = entries.last() as ConstraintBuilderEntry.UniqueForEntry

        assertNull(unscoped.scope)
        assertTrue(scoped.scope!!.perEventSourceType)
    }
}
