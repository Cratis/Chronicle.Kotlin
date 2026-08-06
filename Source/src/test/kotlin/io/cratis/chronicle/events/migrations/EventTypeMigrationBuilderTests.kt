// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events.migrations

import io.cratis.chronicle.events.EventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@EventType(generation = 2)
private data class PersonV2(val fullName: String, val country: String)

@EventType(generation = 1)
private data class PersonV1(val name: String)

class EventTypeMigrationBuilderTests {

    @Test
    fun `empty builder produces empty json object`() {
        val builder = EventTypeMigrationBuilder<PersonV2, PersonV1>()
        assertEquals("{}", builder.toJson())
    }

    @Test
    fun `renamedFrom produces a rename directive keyed by the target property`() {
        val builder = EventTypeMigrationBuilder<PersonV2, PersonV1>()
        builder.renamedFrom(PersonV2::fullName, PersonV1::name)
        assertEquals("""{"fullName":{"${'$'}rename":"name"}}""", builder.toJson())
    }

    @Test
    fun `defaultValue produces a defaultValue directive keyed by the target property`() {
        val builder = EventTypeMigrationBuilder<PersonV2, PersonV1>()
        builder.defaultValue(PersonV2::country, "unknown")
        assertEquals("""{"country":{"${'$'}defaultValue":"unknown"}}""", builder.toJson())
    }

    @Test
    fun `chaining multiple operations keeps them all`() {
        val builder = EventTypeMigrationBuilder<PersonV2, PersonV1>()
        builder.renamedFrom(PersonV2::fullName, PersonV1::name).defaultValue(PersonV2::country, "unknown")
        assertEquals(
            """{"fullName":{"${'$'}rename":"name"},"country":{"${'$'}defaultValue":"unknown"}}""",
            builder.toJson()
        )
    }
}

private class PersonMigration : EventTypeMigration<PersonV2, PersonV1>(PersonV2::class, PersonV1::class) {
    override fun upcast(builder: EventTypeMigrationBuilder<PersonV2, PersonV1>) {
        builder.renamedFrom(PersonV2::fullName, PersonV1::name).defaultValue(PersonV2::country, "unknown")
    }

    override fun downcast(builder: EventTypeMigrationBuilder<PersonV1, PersonV2>) {
        builder.renamedFrom(PersonV1::name, PersonV2::fullName)
    }
}

class EventTypeMigrationTests {

    @Test
    fun `target and source classes are exposed as given to the constructor`() {
        val migration = PersonMigration()
        assertEquals(PersonV2::class, migration.targetClass)
        assertEquals(PersonV1::class, migration.sourceClass)
    }
}
