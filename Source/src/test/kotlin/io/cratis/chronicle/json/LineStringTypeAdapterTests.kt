// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import com.google.gson.JsonParseException
import io.cratis.chronicle.geospatial.LineString
import io.cratis.chronicle.geospatial.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class LineStringTypeAdapterTests {

    private val route = LineString(listOf(Point(10.5, 59.9), Point(10.7, 59.8), Point(10.9, 59.7)))

    @Test
    fun `a line string serializes as a GeoJSON LineString with nested coordinate pairs`() {
        val json = chronicleGson.toJson(route)
        assertEquals(
            """{"type":"LineString","coordinates":[[10.5,59.9],[10.7,59.8],[10.9,59.7]]}""",
            json
        )
    }

    @Test
    fun `a GeoJSON LineString round-trips back to the same line string`() {
        assertEquals(route, chronicleGson.fromJson(chronicleGson.toJson(route), LineString::class.java))
    }

    @Test
    fun `a line string built from loose points equals one built from a list`() {
        assertEquals(route, LineString(Point(10.5, 59.9), Point(10.7, 59.8), Point(10.9, 59.7)))
    }

    @Test
    fun `a line with fewer than two points is rejected`() {
        val json = """{"type":"LineString","coordinates":[[10.5,59.9]]}"""
        assertThrows(JsonParseException::class.java) { chronicleGson.fromJson(json, LineString::class.java) }
    }

    @Test
    fun `a geometry declaring another type is rejected`() {
        val json = """{"type":"Point","coordinates":[[10.5,59.9],[10.7,59.8]]}"""
        assertThrows(JsonParseException::class.java) { chronicleGson.fromJson(json, LineString::class.java) }
    }
}
