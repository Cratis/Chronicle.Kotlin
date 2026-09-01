// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.schemas

import com.google.gson.Gson
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.compliance.PiiNotSupportedOnEventSourceId
import io.cratis.chronicle.concepts.ConceptAs
import io.cratis.chronicle.concepts.EventSourceId
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.geospatial.LineString
import io.cratis.chronicle.geospatial.Point
import io.cratis.chronicle.geospatial.Polygon
import io.cratis.chronicle.readModels.ReadModel
import kotlin.reflect.KClass
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
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
private data class SensitiveValue(override val value: String) : ConceptAs<String>

private data class PersonWithPiiTypedProperty(
    val name: String,
    val secret: SensitiveValue
)

private data class PersonWithPiiList(
    val name: String,
    val secrets: List<SensitiveValue>
)

@Pii(description = "Every property is sensitive")
private data class FullyPiiClass(val value: String, val otherValue: String)

@Pii(description = "Every value here is sensitive")
private data class MedicalDetails(val condition: String, val diagnosedBy: String)

private data class PatientRecord(val name: String, val details: MedicalDetails)

@Pii(description = "The whole envelope is sensitive")
private data class PiiEnvelope(val secret: SensitiveValue)

@Pii(description = "Employee identifier")
private data class PiiEmployeeId(override val value: String) : EventSourceId

private data class EventWithPiiEventSourceId(val employeeId: PiiEmployeeId, val name: String)

@Pii(description = "Employee national identifier")
private data class NationalId(override val value: String) : ConceptAs<String>

@EventType
private data class EmployeeHired(val employeeId: String, val nationalId: NationalId)

@ReadModel
private data class Employee(val id: String, val nationalId: NationalId)

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
    fun `a class-level Pii annotation cascades to every leaf property instead of tagging the container`() {
        val schema = parse(FullyPiiClass::class)
        assertFalse(schema.containsKey("compliance"))

        val properties = propertiesOf(FullyPiiClass::class)
        val value = properties["value"] as Map<*, *>
        val otherValue = properties["otherValue"] as Map<*, *>
        listOf(value, otherValue).forEach {
            val compliance = complianceOf(it)
            assertEquals(1, compliance.size)
            assertEquals("PII", compliance[0]["metadataType"])
            assertEquals("Every property is sensitive", compliance[0]["details"])
        }
    }

    @Test
    fun `a Pii-marked nested value object descends to its own leaves, not the container`() {
        val properties = propertiesOf(PatientRecord::class)
        val details = properties["details"] as Map<*, *>
        assertFalse(details.containsKey("compliance"))

        @Suppress("UNCHECKED_CAST")
        val detailsProperties = details["properties"] as Map<String, Any?>
        val condition = detailsProperties["condition"] as Map<*, *>
        val diagnosedBy = detailsProperties["diagnosedBy"] as Map<*, *>
        assertEquals(1, complianceOf(condition).size)
        assertEquals(1, complianceOf(diagnosedBy).size)
    }

    @Test
    fun `duplicate Pii markers reaching the same leaf produce exactly one compliance entry`() {
        val properties = propertiesOf(PiiEnvelope::class)
        val secret = properties["secret"] as Map<*, *>
        val compliance = complianceOf(secret)
        assertEquals(1, compliance.size)
        assertEquals("PII", compliance[0]["metadataType"])
    }

    @Test
    fun `a Pii annotation on an EventSourceId-typed concept is rejected`() {
        assertThrows(PiiNotSupportedOnEventSourceId::class.java) {
            JsonSchemaGenerator.generate(EventWithPiiEventSourceId::class)
        }
    }

    @Test
    fun `a Pii-marked concept used by an event carries compliance metadata`() {
        val properties = propertiesOf(EmployeeHired::class)
        val nationalId = properties["nationalId"] as Map<*, *>
        val compliance = complianceOf(nationalId)
        assertEquals(1, compliance.size)
        assertEquals("PII", compliance[0]["metadataType"])
        assertEquals("Employee national identifier", compliance[0]["details"])
    }

    @Test
    fun `a Pii-marked concept used by a read model carries compliance metadata`() {
        val properties = propertiesOf(Employee::class)
        val nationalId = properties["nationalId"] as Map<*, *>
        val compliance = complianceOf(nationalId)
        assertEquals(1, compliance.size)
        assertEquals("PII", compliance[0]["metadataType"])
        assertEquals("Employee national identifier", compliance[0]["details"])
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
