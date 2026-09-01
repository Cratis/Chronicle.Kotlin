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
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID
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

private data class Numbers(
    val small: Short,
    val medium: Int,
    val large: Long,
    val single: Float,
    val precise: Double,
    val octet: Byte,
    val price: BigDecimal
)

private data class ScalarFormats(
    val id: UUID,
    val decidedAt: LocalDateTime,
    val validUntil: OffsetDateTime,
    val recordedAt: Instant,
    val startDate: LocalDate,
    val startTime: LocalTime,
    val timeout: Duration,
    val payload: ByteArray
)

private data class MemberId(override val value: UUID) : ConceptAs<UUID>

private data class LastSeen(override val value: Instant) : ConceptAs<Instant>

private data class Membership(val member: MemberId, val lastSeen: LastSeen)

@JsonSchemaType(String::class)
private data class Price(val amount: Long, val currency: String)

private data class Invoice(val total: Price)

@JsonSchemaType(SelfReferencing::class)
private data class SelfReferencing(val value: String)

private data class HasSelfReferencing(val ref: SelfReferencing)

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
    fun `generate maps enum properties to integer schemas carrying the ordinal values`() {
        val properties = propertiesOf(Person::class)
        val status = properties["status"] as Map<*, *>
        assertEquals("integer", status["type"])
        @Suppress("UNCHECKED_CAST")
        val enumValues = status["enum"] as List<Double>
        assertEquals(listOf(0.0, 1.0), enumValues)
    }

    @Test
    fun `generate carries the enum constant names alongside the ordinal values as x-enumNames`() {
        val properties = propertiesOf(Person::class)
        val status = properties["status"] as Map<*, *>
        @Suppress("UNCHECKED_CAST")
        val enumNames = status["x-enumNames"] as List<String>
        assertEquals(listOf("Active", "Inactive"), enumNames)
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

    @Test
    fun `generate marks integer properties with their kernel-recognized format`() {
        val properties = propertiesOf(Numbers::class)
        assertEquals("int16", formatOf(properties, "small"))
        assertEquals("int32", formatOf(properties, "medium"))
        assertEquals("int64", formatOf(properties, "large"))
        assertEquals("byte", formatOf(properties, "octet"))
        listOf("small", "medium", "large", "octet").forEach { assertEquals("integer", typeOf(properties, it)) }
    }

    @Test
    fun `generate marks floating point and decimal properties with their kernel-recognized format`() {
        val properties = propertiesOf(Numbers::class)
        assertEquals("float", formatOf(properties, "single"))
        assertEquals("double", formatOf(properties, "precise"))
        assertEquals("decimal", formatOf(properties, "price"))
        listOf("single", "precise", "price").forEach { assertEquals("number", typeOf(properties, it)) }
    }

    @Test
    fun `generate marks a UUID property as a guid-formatted string`() {
        val properties = propertiesOf(ScalarFormats::class)
        assertEquals("string", typeOf(properties, "id"))
        assertEquals("guid", formatOf(properties, "id"))
    }

    @Test
    fun `generate marks date and time properties with their kernel-recognized format`() {
        val properties = propertiesOf(ScalarFormats::class)
        assertEquals("date-time", formatOf(properties, "decidedAt"))
        assertEquals("date-time-offset", formatOf(properties, "validUntil"))
        assertEquals("date-time-offset", formatOf(properties, "recordedAt"))
        assertEquals("date", formatOf(properties, "startDate"))
        assertEquals("time", formatOf(properties, "startTime"))
        assertEquals("duration", formatOf(properties, "timeout"))
        listOf("decidedAt", "validUntil", "recordedAt", "startDate", "startTime", "timeout")
            .forEach { assertEquals("string", typeOf(properties, it)) }
    }

    @Test
    fun `generate marks a ByteArray property as a byte-array-formatted string`() {
        val properties = propertiesOf(ScalarFormats::class)
        assertEquals("string", typeOf(properties, "payload"))
        assertEquals("byte-array", formatOf(properties, "payload"))
    }

    @Test
    fun `a concept wrapping a UUID carries the guid format and the wrapped scalar type`() {
        val properties = propertiesOf(Membership::class)
        val member = properties["member"] as Map<*, *>
        assertEquals("string", member["type"])
        assertEquals("guid", member["format"])
    }

    @Test
    fun `a concept wrapping an Instant carries the date-time-offset format`() {
        val properties = propertiesOf(Membership::class)
        val lastSeen = properties["lastSeen"] as Map<*, *>
        assertEquals("string", lastSeen["type"])
        assertEquals("date-time-offset", lastSeen["format"])
    }

    @Test
    fun `a type annotated with JsonSchemaType is represented as its declared override type`() {
        val properties = propertiesOf(Invoice::class)
        val total = properties["total"] as Map<*, *>
        assertEquals("string", total["type"])
        assertFalse(total.containsKey("properties"))
    }

    @Test
    fun `a type annotated with JsonSchemaType pointing at itself throws`() {
        assertThrows(SelfReferencingJsonSchemaType::class.java) {
            JsonSchemaGenerator.generate(HasSelfReferencing::class)
        }
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
