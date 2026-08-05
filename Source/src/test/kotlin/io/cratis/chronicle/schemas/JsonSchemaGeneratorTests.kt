// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.schemas

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private data class Employee(
    val firstName: String,
    val lastName: String,
    val age: Int,
    val salary: Double,
    val active: Boolean
)

private class Empty

class JsonSchemaGeneratorTests {

    private fun schemaFor(cls: kotlin.reflect.KClass<*>): JsonObject =
        Gson().fromJson(JsonSchemaGenerator.generate(cls), JsonObject::class.java)

    private fun JsonObject.typeOf(property: String): String =
        getAsJsonObject("properties").getAsJsonObject(property).get("type").asString

    @Test
    fun `schema is an object`() {
        assertEquals("object", schemaFor(Employee::class).get("type").asString)
    }

    @Test
    fun `schema has a property for every member property`() {
        val properties = schemaFor(Employee::class).getAsJsonObject("properties")
        assertEquals(
            setOf("firstName", "lastName", "age", "salary", "active"),
            properties.keySet()
        )
    }

    @Test
    fun `string property maps to string`() {
        assertEquals("string", schemaFor(Employee::class).typeOf("firstName"))
    }

    @Test
    fun `int property maps to integer`() {
        assertEquals("integer", schemaFor(Employee::class).typeOf("age"))
    }

    @Test
    fun `double property maps to number`() {
        assertEquals("number", schemaFor(Employee::class).typeOf("salary"))
    }

    @Test
    fun `boolean property maps to boolean`() {
        assertEquals("boolean", schemaFor(Employee::class).typeOf("active"))
    }

    @Test
    fun `class without properties gets an empty properties object`() {
        val schema = schemaFor(Empty::class)
        assertEquals("object", schema.get("type").asString)
        assertTrue(schema.getAsJsonObject("properties").keySet().isEmpty())
    }
}
