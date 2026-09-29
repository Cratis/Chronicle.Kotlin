// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private data class EmployeeEmailSet(val email: String, val name: String)
private data class EmployeeMoved(val address: String)
private data class PersonRegistered(val firstName: String, val lastName: String)
private data class PersonRenamed(val firstName: String, val lastName: String)

class ConstraintBuilderTests {

    @Test
    fun `on resolves the property actually passed to it`() {
        val builder = UniqueConstraintBuilder()
        builder.on(EmployeeEmailSet::class, EmployeeEmailSet::email)

        assertEquals(listOf("email"), builder.build().eventDefinitions.single().properties)
    }

    @Test
    fun `on with a different property produces a constraint keyed on that property, not the first declared field`() {
        val emailBuilder = UniqueConstraintBuilder()
        emailBuilder.on(EmployeeEmailSet::class, EmployeeEmailSet::email)

        val nameBuilder = UniqueConstraintBuilder()
        nameBuilder.on(EmployeeEmailSet::class, EmployeeEmailSet::name)

        val emailEntry = emailBuilder.build()
        val nameEntry = nameBuilder.build()

        assertEquals(listOf("email"), emailEntry.eventDefinitions.single().properties)
        assertEquals(listOf("name"), nameEntry.eventDefinitions.single().properties)
        assertNotEquals(emailEntry.eventDefinitions, nameEntry.eventDefinitions)
    }

    @Test
    fun `unique DSL resolves the configured property through the fluent builder`() {
        val builder = ConstraintBuilder()
        builder.unique { it.on(EmployeeEmailSet::class, EmployeeEmailSet::name).ignoreCasing() }

        val entry = builder.build().single() as ConstraintBuilderEntry.UniqueEntry

        assertEquals(UniqueEventDefinition(EmployeeEmailSet::class, listOf("name")), entry.eventDefinitions.single())
        assertTrue(entry.ignoreCasing)
    }

    @Test
    fun `onWithPropertyName resolves an explicit property name, for Java callers that cannot supply a KProperty1`() {
        val builder = UniqueConstraintBuilder()
        builder.onWithPropertyName(EmployeeEmailSet::class, "name")

        assertEquals(listOf("name"), builder.build().eventDefinitions.single().properties)
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

    @Test
    fun `on with several properties keeps all of them, in order, on one event definition`() {
        val builder = UniqueConstraintBuilder()
        builder.on(PersonRegistered::class, PersonRegistered::firstName, PersonRegistered::lastName).ignoreCasing()

        val entry = builder.build()

        assertEquals(
            listOf(UniqueEventDefinition(PersonRegistered::class, listOf("firstName", "lastName"))),
            entry.eventDefinitions
        )
        assertTrue(entry.ignoreCasing)
    }

    @Test
    fun `onWithPropertyNames keeps all the names, for Java callers`() {
        val builder = UniqueConstraintBuilder()
        builder.onWithPropertyNames(PersonRegistered::class, "firstName", "lastName")

        assertEquals(listOf("firstName", "lastName"), builder.build().eventDefinitions.single().properties)
    }

    @Test
    fun `on for different event types adds one definition per event type`() {
        val builder = UniqueConstraintBuilder()
        builder
            .on(PersonRegistered::class, PersonRegistered::firstName, PersonRegistered::lastName)
            .on(PersonRenamed::class, PersonRenamed::firstName, PersonRenamed::lastName)

        assertEquals(
            listOf(
                UniqueEventDefinition(PersonRegistered::class, listOf("firstName", "lastName")),
                UniqueEventDefinition(PersonRenamed::class, listOf("firstName", "lastName"))
            ),
            builder.build().eventDefinitions
        )
    }

    @Test
    fun `on for the same event type twice throws instead of the second call replacing the first`() {
        val builder = UniqueConstraintBuilder()
        builder.on(PersonRegistered::class, PersonRegistered::firstName)

        val error = assertThrows(EventTypeAlreadyAddedToUniqueConstraint::class.java) {
            builder.on(PersonRegistered::class, PersonRegistered::lastName)
        }

        assertEquals(PersonRegistered::class, error.eventClass)
        assertEquals(listOf("lastName"), error.properties)
    }

    @Test
    fun `mixing the property reference and property name overloads for one event type also throws`() {
        val builder = UniqueConstraintBuilder()
        builder.on(PersonRegistered::class, PersonRegistered::firstName)

        assertThrows(EventTypeAlreadyAddedToUniqueConstraint::class.java) {
            builder.onWithPropertyNames(PersonRegistered::class, "lastName")
        }
    }

    @Test
    fun `building with no event types added throws`() {
        assertThrows(NoEventTypesAddedToUniqueConstraint::class.java) { UniqueConstraintBuilder().build() }
    }

    @Test
    fun `unique with nothing configured throws through the constraint builder`() {
        assertThrows(NoEventTypesAddedToUniqueConstraint::class.java) { ConstraintBuilder().unique { } }
    }

    @Test
    fun `an event definition with no properties is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { UniqueConstraintBuilder().on(PersonRegistered::class) }
    }

    @Test
    fun `scope applies to a multi event definition constraint and keeps every definition`() {
        val builder = ConstraintBuilder()
        builder.perEventSourceType().unique {
            it.on(PersonRegistered::class, PersonRegistered::firstName, PersonRegistered::lastName)
                .on(PersonRenamed::class, PersonRenamed::firstName, PersonRenamed::lastName)
        }

        val entry = builder.build().single() as ConstraintBuilderEntry.UniqueEntry

        assertEquals(2, entry.eventDefinitions.size)
        assertTrue(entry.scope!!.perEventSourceType)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `the single property shape of a unique entry still constructs and reads back`() {
        val entry = ConstraintBuilderEntry.UniqueEntry(EmployeeEmailSet::class, "email", true, "Taken")

        assertEquals(EmployeeEmailSet::class, entry.eventClass)
        assertEquals("email", entry.propertyName)
        assertEquals(listOf(UniqueEventDefinition(EmployeeEmailSet::class, listOf("email"))), entry.eventDefinitions)
        assertTrue(entry.ignoreCasing)
        assertEquals("Taken", entry.message)
        assertNull(entry.scope)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `the legacy copy keeps every event definition unless it is given a different event type or property`() {
        val entry = ConstraintBuilderEntry.UniqueEntry(
            listOf(
                UniqueEventDefinition(PersonRegistered::class, listOf("firstName", "lastName")),
                UniqueEventDefinition(PersonRenamed::class, listOf("firstName"))
            ),
            false,
            ""
        )

        assertEquals(entry.eventDefinitions, entry.copy(ignoreCasing = true).eventDefinitions)
        assertEquals(
            listOf(UniqueEventDefinition(EmployeeMoved::class, listOf("address"))),
            entry.copy(eventClass = EmployeeMoved::class, propertyName = "address").eventDefinitions
        )
    }
}
