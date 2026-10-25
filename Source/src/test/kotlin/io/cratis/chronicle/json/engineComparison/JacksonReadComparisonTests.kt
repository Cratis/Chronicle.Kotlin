// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json.engineComparison

import io.cratis.chronicle.geospatial.Point
import io.cratis.chronicle.json.chronicleGson
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

private data class ReadCase(val name: String, val type: Class<*>, val json: String, val expectedJson: String, val sample: Any? = null)

private fun historical(name: String, type: Class<*>, expectedJson: String): ReadCase =
    ReadCase(name, type, ComparisonFixtures.resource("historical", name), expectedJson)

private fun readCases(): List<ReadCase> = ComparisonFixtures.all.map {
    ReadCase("golden-${it.name}", it.type, ComparisonFixtures.resource("golden", it.name), chronicleGson.toJson(it.sample), it.sample)
} + listOf(
    historical("missing-string", HistoricalRequired::class.java, """{"name":"historical","count":2}"""),
    historical("missing-int", HistoricalRequired::class.java, """{"name":"historical","count":2}"""),
    historical("missing-default", HistoricalDefault::class.java, """{"name":"historical","count":7}"""),
    historical("unknown-field", HistoricalRequired::class.java, """{"name":"historical","count":2}"""),
    historical("null-primitive", HistoricalRequired::class.java, """{"name":"historical","count":2}"""),
    historical("private-kotlin", HistoricalPrivate::class.java, chronicleGson.toJson(HistoricalPrivate.sample())),
    historical("private-java", ComparisonPojo.PrivateConstructor::class.java, """{"name":"historical"}"""),
    historical("no-default-java", ComparisonPojo.NoDefaultConstructor::class.java, """{"name":"historical"}"""),
    historical("enum-name", ComparisonStatus::class.java, "1"),
    historical("enum-lowercase", ComparisonStatus::class.java, "1"),
    historical("enum-number-string", ComparisonStatus::class.java, "1"),
    historical("dotnet-instant", Instant::class.java, "\"2024-05-01T10:00:00Z\""),
    historical("dotnet-offset", OffsetDateTime::class.java, "\"2024-05-01T10:00:00Z\""),
    historical("local-datetime-offset", LocalDateTime::class.java, "\"2024-05-01T10:00:00\""),
    historical("dotnet-time", LocalTime::class.java, "\"10:00:00\""),
    historical("geo-reordered", Point::class.java, """{"type":"Point","coordinates":[10.5,59.9]}""")
)

class JacksonReadComparisonTests {
    @TestFactory
    fun `every golden and historical read outcome is recorded`(): List<DynamicTest> = readCases().flatMap { case ->
        (listOf("gson") + comparisonMappers.keys).map { engine ->
            DynamicTest.dynamicTest("$engine ${case.name}") {
                assertEquals(expectedReadOutcomes.getValue(case.name).getValue(engine), outcome(case, engine))
            }
        }
    }
}

private fun outcome(case: ReadCase, engine: String): String {
    val value = try {
        if (engine == "gson") chronicleGson.fromJson(case.json, case.type)
        else comparisonMappers.getValue(engine).readValue(case.json, case.type)
    } catch (exception: Exception) {
        return "Throws(${exception.javaClass.simpleName})"
    }
    val differences = jsonTreeDiff(case.expectedJson, chronicleGson.toJson(value))
        .filterNot { ": order " in it }.toMutableList()
    // JSON cannot reveal the region-zone identity lost by the offset-only wire representation.
    val expectedZone = (case.sample as? ComparisonTemporal)?.zoned?.zone
    if (expectedZone != null) {
        val actualZone = (value as? ComparisonTemporal)?.zoned?.zone
        if (expectedZone != actualZone) differences.add("$.zoned.zone: value $expectedZone -> $actualZone")
    }
    return if (differences.isEmpty()) "Equal" else differs(*differences.toTypedArray())
}

private fun reads(gson: String = "Equal", fundamentals: String = "Equal", parity: String = "Equal"): Map<String, String> =
    mapOf("gson" to gson, "fundamentals" to fundamentals, "gsonParity" to parity)

private fun differs(vararg differences: String): String = "Differs(${differences.sorted().joinToString()})"

private val conceptKeyDifference = differs(
    "$.keys.StringConcept(value=StringConcept(value=key)): extra <absent> -> \"value\"",
    "$.keys.StringConcept(value=key): missing \"value\" -> <absent>"
)
private const val zoneDifference = "$.zoned.zone: value Europe/Oslo -> +02:00"

private val expectedReadOutcomes: Map<String, Map<String, String>> = mapOf(
    "golden-event" to reads(fundamentals = "Throws(MismatchedInputException)"), // WIRE
    "golden-read-model" to reads(),
    "golden-concepts" to reads(conceptKeyDifference, conceptKeyDifference, conceptKeyDifference), // JVM-ONLY
    // $.zoned.zone: JVM-ONLY (region lost by all readers); $.offset and $.zoned: WIRE (UTC adjustment).
    "golden-temporal" to reads(
        differs(zoneDifference),
        differs(
            "$.offset: value \"2024-05-01T12:00:00.123456789+02:00\" -> \"2024-05-01T10:00:00.123456789Z\"",
            "$.zoned: value \"2024-05-01T12:00:00.123456789+02:00\" -> \"2024-05-01T10:00:00.123456789Z\"",
            "$.zoned.zone: value Europe/Oslo -> Z"
        ),
        differs(zoneDifference)
    ),
    "golden-geospatial" to reads(fundamentals = "Throws(MismatchedInputException)"), // WIRE
    "golden-enums" to reads(),
    "golden-nulls" to reads(),
    "golden-java-pojo" to reads(),
    "golden-java-record" to reads(),
    // Gson allocates without a constructor: name is actually null despite Kotlin's non-null declaration.
    "missing-string" to reads(differs("$.name: missing \"historical\" -> <absent>"), "Throws(KotlinInvalidNullException)", "Throws(KotlinInvalidNullException)"), // JVM-ONLY
    "missing-int" to reads(differs("$.count: value 2 -> 0"), "Throws(MismatchedInputException)", "Throws(MismatchedInputException)"), // JVM-ONLY
    "missing-default" to reads(gson = differs("$.count: value 7 -> 0")), // JVM-ONLY
    "unknown-field" to reads(),
    "null-primitive" to reads(differs("$.count: value 2 -> 0"), "Throws(MismatchedInputException)", "Throws(MismatchedInputException)"), // JVM-ONLY
    "private-kotlin" to reads(),
    "private-java" to reads(fundamentals = differs("$.name: missing \"historical\" -> <absent>")), // JVM-ONLY
    "no-default-java" to reads(fundamentals = "Throws(MismatchedInputException)", parity = "Throws(MismatchedInputException)"), // JVM-ONLY
    "enum-name" to reads(),
    "enum-lowercase" to reads(),
    "enum-number-string" to reads(),
    "dotnet-instant" to reads(),
    "dotnet-offset" to reads(),
    "local-datetime-offset" to reads(fundamentals = "Throws(InvalidFormatException)", parity = "Throws(InvalidFormatException)"), // WIRE
    "dotnet-time" to reads(),
    "geo-reordered" to reads(fundamentals = "Throws(MismatchedInputException)") // WIRE
)
