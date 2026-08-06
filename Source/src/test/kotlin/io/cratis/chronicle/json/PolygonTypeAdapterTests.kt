// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import com.google.gson.JsonParseException
import io.cratis.chronicle.geospatial.LinearRing
import io.cratis.chronicle.geospatial.Point
import io.cratis.chronicle.geospatial.Polygon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PolygonTypeAdapterTests {

    private val shell = LinearRing(Point(0.0, 0.0), Point(4.0, 0.0), Point(4.0, 4.0), Point(0.0, 0.0))
    private val hole = LinearRing(Point(1.0, 1.0), Point(2.0, 1.0), Point(2.0, 2.0), Point(1.0, 1.0))

    @Test
    fun `a solid polygon serializes as a GeoJSON Polygon holding only its shell`() {
        val json = chronicleGson.toJson(Polygon(shell))
        assertEquals(
            """{"type":"Polygon","coordinates":[[[0.0,0.0],[4.0,0.0],[4.0,4.0],[0.0,0.0]]]}""",
            json
        )
    }

    @Test
    fun `a polygon writes its holes after the shell`() {
        val json = chronicleGson.toJson(Polygon(shell, listOf(hole)))
        assertEquals(
            """{"type":"Polygon","coordinates":""" +
                """[[[0.0,0.0],[4.0,0.0],[4.0,4.0],[0.0,0.0]],[[1.0,1.0],[2.0,1.0],[2.0,2.0],[1.0,1.0]]]}""",
            json
        )
    }

    @Test
    fun `a solid GeoJSON Polygon round-trips back to the same polygon`() {
        val polygon = Polygon(shell)
        assertEquals(polygon, chronicleGson.fromJson(chronicleGson.toJson(polygon), Polygon::class.java))
    }

    @Test
    fun `a GeoJSON Polygon with holes round-trips with the shell and holes intact`() {
        val polygon = Polygon(shell, listOf(hole))
        val roundTripped = chronicleGson.fromJson(chronicleGson.toJson(polygon), Polygon::class.java)
        assertEquals(shell, roundTripped.shell)
        assertEquals(listOf(hole), roundTripped.holes)
    }

    @Test
    fun `a ring with fewer than four points is rejected`() {
        val json = """{"type":"Polygon","coordinates":[[[0.0,0.0],[4.0,0.0],[0.0,0.0]]]}"""
        assertThrows(JsonParseException::class.java) { chronicleGson.fromJson(json, Polygon::class.java) }
    }

    @Test
    fun `a ring that does not return to its first point is rejected`() {
        val json = """{"type":"Polygon","coordinates":[[[0.0,0.0],[4.0,0.0],[4.0,4.0],[0.0,4.0]]]}"""
        assertThrows(JsonParseException::class.java) { chronicleGson.fromJson(json, Polygon::class.java) }
    }

    @Test
    fun `a polygon without any ring is rejected`() {
        assertThrows(JsonParseException::class.java) {
            chronicleGson.fromJson("""{"type":"Polygon","coordinates":[]}""", Polygon::class.java)
        }
    }
}
