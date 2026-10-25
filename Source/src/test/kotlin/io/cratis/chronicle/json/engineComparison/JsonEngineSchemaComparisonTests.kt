// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json.engineComparison

import com.google.gson.JsonParser
import io.cratis.chronicle.eventSequences.AppendError
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.ConstraintViolation
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.json.chronicleGson
import io.cratis.chronicle.schemas.JsonSchemaGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Cheap client-side evidence; these assertions do not replace kernel schema validation. */
class JsonEngineSchemaComparisonTests {
    @Test
    fun `schema rejection checks extract constraint violations even when append errors are empty`() {
        // Reproduce CI's result channel and observed mismatches, not a verbatim server response.
        // Split evidence across message and details to exercise both fields without assuming detail keys.
        val result = AppendResult(
            sequenceNumber = EventSequenceNumber.first,
            constraintViolations = listOf(
                ConstraintViolation("SchemaValidation", "counts", mapOf("expected" to "string", "actual" to "object")),
                ConstraintViolation("SchemaValidation", "expected string got null", mapOf("property" to "optional")),
                ConstraintViolation("SchemaValidation", "count expected integer got string")
            ),
            errors = emptyList(),
            isSuccess = false
        )
        assertTrue(result.errors.isEmpty())
        assertFalse(result.isSuccess)
        assertTrue(result.hasSchemaValidationViolation("counts", "string", "object"))
        assertTrue(result.hasSchemaValidationViolation("optional", "string", "null"))
        assertTrue(result.hasSchemaValidationViolation("count", "integer", "string"))
        assertFalse(result.hasSchemaValidationViolation("counts", "integer", "object"))
    }

    @Test
    fun `append errors and unrelated constraints cannot satisfy schema rejection checks`() {
        val description = "counts expected string got object"
        val result = AppendResult(
            sequenceNumber = EventSequenceNumber.first,
            constraintViolations = listOf(ConstraintViolation("OtherConstraint", description)),
            errors = listOf(AppendError("SchemaValidation $description")),
            isSuccess = false
        )
        assertFalse(result.hasSchemaValidationViolation("counts", "string", "object"))
        assertFalse(result.copy(constraintViolations = emptyList()).hasSchemaValidationViolation("counts", "string", "object"))
    }

    @Test
    fun `the original schema describes a map and nullable string as strings despite object and null output`() {
        val schema = JsonSchemaGenerator.generate(JsonEngineComparisonEvent::class)
        println("[json-engine-schema] complex: $schema")
        val properties = JsonParser.parseString(schema).asJsonObject.getAsJsonObject("properties")
        assertEquals("string", properties.getAsJsonObject("counts").get("type").asString)
        assertEquals("string", properties.getAsJsonObject("optional").get("type").asString)
        val payloads = linkedMapOf("gson" to chronicleGson.toJson(ComparisonFixtures.event))
        comparisonMappers.forEach { (name, mapper) -> payloads[name] = mapper.writeValueAsString(ComparisonFixtures.event) }
        payloads.forEach { (name, json) ->
            val content = JsonParser.parseString(json).asJsonObject
            assertTrue(content.get("counts").isJsonObject, "$name: $json")
            if (name == "fundamentals") {
                assertTrue(content.get("optional").isJsonNull, json)
            } else {
                assertFalse(content.has("optional"), "$name: $json")
            }
        }
    }

    @Test
    fun `the separate simple event has representative schema types and matching engine payloads`() {
        val schema = JsonSchemaGenerator.generate(JsonEngineComparisonSimpleEvent::class)
        println("[json-engine-schema] simple: $schema")
        val properties = JsonParser.parseString(schema).asJsonObject.getAsJsonObject("properties")
        val types = properties.entrySet().associate { (name, property) -> name to property.asJsonObject.get("type").asString }
        assertEquals(mapOf("name" to "string", "count" to "integer", "tags" to "array", "nested" to "object", "concept" to "string", "status" to "integer", "at" to "string"), types)
        assertEquals("string", properties.getAsJsonObject("tags").getAsJsonObject("items").get("type").asString)
        val nested = properties.getAsJsonObject("nested").getAsJsonObject("properties")
        assertEquals("string", nested.getAsJsonObject("label").get("type").asString)
        assertEquals("integer", nested.getAsJsonObject("quantity").get("type").asString)
        assertEquals("date-time-offset", properties.getAsJsonObject("at").get("format").asString)
        val event = ComparisonFixtures.simpleEvent
        val gsonJson = chronicleGson.toJson(event)
        assertEquals(event, chronicleGson.fromJson(gsonJson, JsonEngineComparisonSimpleEvent::class.java))
        comparisonMappers.forEach { (name, mapper) ->
            val json = mapper.writeValueAsString(event)
            assertEquals(emptyList<String>(), jsonTreeDiff(gsonJson, json), name)
            assertEquals(event, chronicleGson.fromJson(json, JsonEngineComparisonSimpleEvent::class.java), name)
            assertEquals(json, chronicleGson.toJson(RawSimpleEventContent(json)), "raw wrapper: $name")
        }
    }

    @Test
    fun `the simple negative control changes only integer count to a string`() {
        val original = JsonParser.parseString(chronicleGson.toJson(ComparisonFixtures.simpleEvent)).asJsonObject
        val negative = JsonParser.parseString(ComparisonFixtures.invalidCountJson(ComparisonFixtures.simpleEvent)).asJsonObject
        assertTrue(original.getAsJsonPrimitive("count").isNumber)
        assertTrue(negative.getAsJsonPrimitive("count").isString)
        assertEquals("not-a-number", negative.remove("count").asString)
        original.remove("count")
        assertEquals(original, negative)
    }
}
