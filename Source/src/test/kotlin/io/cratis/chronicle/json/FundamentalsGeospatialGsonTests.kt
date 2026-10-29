// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

@file:Suppress("DEPRECATION")

package io.cratis.chronicle.json

import com.google.gson.JsonParseException
import io.cratis.chronicle.geospatial.LineString as LegacyLineString
import io.cratis.chronicle.geospatial.LinearRing as LegacyLinearRing
import io.cratis.chronicle.geospatial.Point as LegacyPoint
import io.cratis.chronicle.geospatial.Polygon as LegacyPolygon
import io.cratis.fundamentals.geospatial.LineString
import io.cratis.fundamentals.geospatial.LinearRing
import io.cratis.fundamentals.geospatial.Point
import io.cratis.fundamentals.geospatial.Polygon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class FundamentalsGeospatialGsonTests {

    private data class Geometries(val point: Point, val line: LineString, val polygon: Polygon)
    private data class Optional(val point: Point?, val polygon: Polygon?)
    private data class Mixed(val legacy: LegacyPoint, val fundamentals: Point)

    private val shell = LinearRing(Point(0.0, 0.0), Point(4.0, 0.0), Point(4.0, 4.0), Point(0.0, 0.0))
    private val hole = LinearRing(Point(1.0, 1.0), Point(2.0, 1.0), Point(2.0, 2.0), Point(1.0, 1.0))
    private val legacyShell = LegacyLinearRing(LegacyPoint(0.0, 0.0), LegacyPoint(4.0, 0.0), LegacyPoint(4.0, 4.0), LegacyPoint(0.0, 0.0))
    private val legacyHole = LegacyLinearRing(LegacyPoint(1.0, 1.0), LegacyPoint(2.0, 1.0), LegacyPoint(2.0, 2.0), LegacyPoint(1.0, 1.0))

    @Test
    fun `each type writes the bytes the legacy type writes`() {
        assertEquals(chronicleGson.toJson(LegacyPoint(10.5, 59.9)), chronicleGson.toJson(Point(10.5, 59.9)))
        assertEquals(
            chronicleGson.toJson(LegacyLineString(LegacyPoint(1.0, 2.0), LegacyPoint(3.0, 4.0))),
            chronicleGson.toJson(LineString(Point(1.0, 2.0), Point(3.0, 4.0)))
        )
        assertEquals(chronicleGson.toJson(LegacyPolygon(legacyShell)), chronicleGson.toJson(Polygon(shell)))
        assertEquals(
            chronicleGson.toJson(LegacyPolygon(legacyShell, listOf(legacyHole))),
            chronicleGson.toJson(Polygon(shell, listOf(hole)))
        )
    }

    @Test
    fun `values match the engine comparison golden`() {
        val golden = checkNotNull(javaClass.getResourceAsStream("/json-engine-comparison/golden/geospatial.json"))
            .use { it.readBytes().decodeToString().trim() }
        val value = Geometries(Point(10.5, 59.9), LineString(Point(1.0, 2.0), Point(3.0, 4.0)), Polygon(shell))
        assertEquals(golden, chronicleGson.toJson(value))
        assertEquals(value, chronicleGson.fromJson(golden, Geometries::class.java))
    }

    @Test
    fun `values round-trip to equal Fundamentals values`() {
        val polygon = Polygon(shell, listOf(hole))
        assertEquals(polygon, chronicleGson.fromJson(chronicleGson.toJson(polygon), Polygon::class.java))
        val point = Point(10.5, 59.9)
        assertEquals(point, chronicleGson.fromJson(chronicleGson.toJson(point), Point::class.java))
        val line = LineString(Point(1.0, 2.0), Point(3.0, 4.0))
        assertEquals(line, chronicleGson.fromJson(chronicleGson.toJson(line), LineString::class.java))
    }

    @Test
    fun `null properties stay null`() {
        assertEquals("""{}""", chronicleGson.toJson(Optional(null, null)))
        val read = chronicleGson.fromJson("""{"point":null,"polygon":null}""", Optional::class.java)
        assertNull(read.point)
        assertNull(read.polygon)
    }

    @Test
    fun `invalid GeoJSON is rejected with the legacy messages`() {
        val wrongType = assertThrows(JsonParseException::class.java) {
            chronicleGson.fromJson("""{"type":"LineString","coordinates":[1,2]}""", Point::class.java)
        }
        val legacy = assertThrows(JsonParseException::class.java) {
            chronicleGson.fromJson("""{"type":"LineString","coordinates":[1,2]}""", LegacyPoint::class.java)
        }
        assertEquals(legacy.message, wrongType.message)
        assertThrows(JsonParseException::class.java) {
            chronicleGson.fromJson("""{"type":"Polygon","coordinates":[[[0.0,0.0],[4.0,0.0],[0.0,0.0]]]}""", Polygon::class.java)
        }
    }

    @Test
    fun `an event can hold a legacy and a Fundamentals field`() {
        val json = chronicleGson.toJson(Mixed(LegacyPoint(1.0, 2.0), Point(1.0, 2.0)))
        val geo = """{"type":"Point","coordinates":[1.0,2.0]}"""
        assertEquals("""{"legacy":$geo,"fundamentals":$geo}""", json)
        assertEquals(Mixed(LegacyPoint(1.0, 2.0), Point(1.0, 2.0)), chronicleGson.fromJson(json, Mixed::class.java))
    }
}
