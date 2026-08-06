// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.schemas

import com.google.gson.Gson
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.geospatial.LineString
import io.cratis.chronicle.geospatial.Point
import io.cratis.chronicle.geospatial.Polygon
import kotlin.reflect.KClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private val gson = Gson()

private enum class Status { Active, Inactive }

private data class Address(val city: String, val postalCode: String)

private data class Person(
    val name: String,
    val age: Int,
    val score: Double,
    val active: Boolean,
    val address: Address,
    val tags: List<String>,
    val status: Status
)

private data class PersonWithPii(
    val name: String,
    @Pii(description = "Contact email")
    val email: String
)

@Pii(description = "Wraps a sensitive value")
private data class SensitiveValue(val value: String)

private data class PersonWithPiiTypedProperty(
    val name: String,
    val secret: SensitiveValue
)

private data class PersonWithPiiList(
    val name: String,
    val secrets: List<SensitiveValue>
)

@Pii(description = "Every property is sensitive")
private data class FullyPiiClass(val value: String)

private data class Territory(
    val name: String,
    val center: Point,
    val route: LineString,
    val area: Polygon
)

class JsonSchemaGeneratorTests {

    @Test
    fun `generate produces a real schema instead of an empty object literal`() {
        val schema = JsonSchemaGenerator.generate(Person::class)
        assertFalse(schema == "{}")
        assertTrue(schema.contains("\"name\""))
    }

    @Test
    fun `generate reflects each property's JSON type`() {
        val properties = propertiesOf(Person::class)
        assertEquals("string", typeOf(properties, "name"))
        assertEquals("integer", typeOf(properties, "age"))
        assertEquals("number", typeOf(properties, "score"))
        assertEquals("boolean", typeOf(properties, "active"))
    }

    @Test
    fun `generate recurses into nested object properties`() {
        val properties = propertiesOf(Person::class)
        val address = properties["address"] as Map<*, *>
        assertEquals("object", address["type"])
        @Suppress("UNCHECKED_CAST")
        val addressProperties = address["properties"] as Map<String, Any?>
        assertTrue(addressProperties.containsKey("city"))
        assertTrue(addressProperties.containsKey("postalCode"))
    }

    @Test
    fun `generate maps collection properties to array schemas with typed items`() {
        val properties = propertiesOf(Person::class)
        val tags = properties["tags"] as Map<*, *>
        assertEquals("array", tags["type"])
        val items = tags["items"] as Map<*, *>
        assertEquals("string", items["type"])
    }

    @Test
    fun `generate maps enum properties to string schemas carrying the enum values`() {
        val properties = propertiesOf(Person::class)
        val status = properties["status"] as Map<*, *>
        assertEquals("string", status["type"])
        @Suppress("UNCHECKED_CAST")
        val enumValues = status["enum"] as List<String>
        assertEquals(listOf("Active", "Inactive"), enumValues)
    }

    @Test
    fun `a property without Pii carries no compliance metadata`() {
        val properties = propertiesOf(PersonWithPii::class)
        val name = properties["name"] as Map<*, *>
        assertFalse(name.containsKey("compliance"))
    }

    @Test
    fun `a property annotated with Pii carries PII compliance metadata`() {
        val properties = propertiesOf(PersonWithPii::class)
        val email = properties["email"] as Map<*, *>
        val compliance = complianceOf(email)
        assertEquals(1, compliance.size)
        assertEquals("PII", compliance[0]["metadataType"])
        assertEquals("Contact email", compliance[0]["details"])
    }

    @Test
    fun `a property whose declared type is Pii-marked also carries compliance metadata`() {
        val properties = propertiesOf(PersonWithPiiTypedProperty::class)
        val secret = properties["secret"] as Map<*, *>
        val compliance = complianceOf(secret)
        assertEquals(1, compliance.size)
        assertEquals("PII", compliance[0]["metadataType"])
    }

    @Test
    fun `a Pii-marked element type carries compliance metadata on the array's items`() {
        val properties = propertiesOf(PersonWithPiiList::class)
        val secrets = properties["secrets"] as Map<*, *>
        val items = secrets["items"] as Map<*, *>
        val compliance = complianceOf(items)
        assertEquals(1, compliance.size)
        assertEquals("PII", compliance[0]["metadataType"])
    }

    @Test
    fun `a class-level Pii annotation tags the generated schema itself`() {
        val schema = parse(FullyPiiClass::class)
        val compliance = complianceOf(schema)
        assertEquals(1, compliance.size)
        assertEquals("PII", compliance[0]["metadataType"])
    }

    @Test
    fun `generate marks geospatial properties with the format the kernel recognizes`() {
        val properties = propertiesOf(Territory::class)
        assertEquals("point", formatOf(properties, "center"))
        assertEquals("linestring", formatOf(properties, "route"))
        assertEquals("polygon", formatOf(properties, "area"))
    }

    @Test
    fun `generate keeps geospatial properties as leaf objects instead of flattening their coordinates`() {
        val properties = propertiesOf(Territory::class)
        val area = properties["area"] as Map<*, *>
        assertEquals("object", area["type"])
        assertFalse(area.containsKey("properties"))
    }

    private fun parse(cls: KClass<*>): Map<String, Any?> {
        @Suppress("UNCHECKED_CAST")
        return gson.fromJson(JsonSchemaGenerator.generate(cls), Map::class.java) as Map<String, Any?>
    }

    private fun propertiesOf(cls: KClass<*>): Map<String, Any?> {
        @Suppress("UNCHECKED_CAST")
        return parse(cls)["properties"] as Map<String, Any?>
    }

    private fun typeOf(properties: Map<String, Any?>, key: String): String? {
        val prop = properties[key] as Map<*, *>
        return prop["type"] as String?
    }

    private fun formatOf(properties: Map<String, Any?>, key: String): String? {
        val prop = properties[key] as Map<*, *>
        return prop["format"] as String?
    }

    private fun complianceOf(schemaNode: Map<*, *>): List<Map<String, String>> {
        @Suppress("UNCHECKED_CAST")
        return (schemaNode["compliance"] as? List<Map<String, String>>).orEmpty()
    }
}
