// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json.engineComparison

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class JacksonWriteComparisonTests {
    @TestFactory
    fun `every write difference is named and classified`(): List<DynamicTest> = comparisonMappers.flatMap { (name, mapper) ->
        ComparisonFixtures.all.map { fixture ->
            DynamicTest.dynamicTest("$name ${fixture.name}") {
                assertEquals(
                    expectedWriteDiffs.getValue(name).getValue(fixture.name),
                    jsonTreeDiff(ComparisonFixtures.resource("golden", fixture.name), mapper.writeValueAsString(fixture.sample))
                )
            }
        }
    }
}

private val emptyFixtures = ComparisonFixtures.all.associate { it.name to emptyList<String>() }
val expectedWriteDiffs: Map<String, Map<String, List<String>>> = mapOf(
    "fundamentals" to emptyFixtures + mapOf(
        "event" to listOf(
            "$.location.coordinates: missing [10.5,59.9] -> <absent>", // WIRE
            "$.location.latitude: extra <absent> -> 59.9", // WIRE
            "$.location.longitude: extra <absent> -> 10.5", // WIRE
            "$.location.type: missing \"Point\" -> <absent>", // WIRE
            "$.optional: extra <absent> -> null" // WIRE
        ),
        "concepts" to listOf(
            "$.keys.\"key\": extra <absent> -> \"value\"", // DOTNET
            "$.keys.StringConcept(value=key): missing \"value\" -> <absent>" // DOTNET
        ),
        "temporal" to listOf(
            "$.time: value \"10:00:00.123456789\" -> \"10:00:00.1234567\"", // DOTNET
            "$.wholeTime: value \"10:00:00\" -> \"10:00:00.0000000\"" // DOTNET
        ),
        "geospatial" to listOf(
            "$.absent: extra <absent> -> null", // WIRE
            "$.line.coordinates[0]: type [1.0,2.0] -> {\"longitude\":1.0,\"latitude\":2.0}", // WIRE
            "$.line.coordinates[1]: type [3.0,4.0] -> {\"longitude\":3.0,\"latitude\":4.0}", // WIRE
            "$.line.type: missing \"LineString\" -> <absent>", // WIRE
            "$.point.coordinates: missing [10.5,59.9] -> <absent>", // WIRE
            "$.point.latitude: extra <absent> -> 59.9", // WIRE
            "$.point.longitude: extra <absent> -> 10.5", // WIRE
            "$.point.type: missing \"Point\" -> <absent>", // WIRE
            "$.polygon.coordinates: missing [[[0.0,0.0],[4.0,0.0],[4.0,4.0],[0.0,0.0]]] -> <absent>", // WIRE
            "$.polygon.holes: extra <absent> -> []", // WIRE
            "$.polygon.shell: extra <absent> -> {\"coordinates\":[{\"longitude\":0.0,\"latitude\":0.0},{\"longitude\":4.0,\"latitude\":0.0},{\"longitude\":4.0,\"latitude\":4.0},{\"longitude\":0.0,\"latitude\":0.0}]}", // WIRE
            "$.polygon.type: missing \"Polygon\" -> <absent>" // WIRE
        ),
        "nulls" to listOf(
            "$.point: extra <absent> -> null", // WIRE
            "$.text: extra <absent> -> null" // WIRE
        ),
        "java-pojo" to listOf(
            "$.count: missing 4 -> <absent>", // JVM-ONLY
            "$.name: missing \"field-only\" -> <absent>" // JVM-ONLY
        )
    ),
    "gsonParity" to emptyFixtures
)
