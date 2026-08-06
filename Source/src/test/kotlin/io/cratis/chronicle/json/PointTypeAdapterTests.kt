// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import com.google.gson.JsonParseException
import io.cratis.chronicle.geospatial.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PointTypeAdapterTests {

    @Test
    fun `a point serializes as a GeoJSON Point with longitude before latitude`() {
        val json = chronicleGson.toJson(Point(10.5, 59.9))
        assertEquals("""{"type":"Point","coordinates":[10.5,59.9]}""", json)
    }

    @Test
    fun `a GeoJSON Point round-trips back to the same point`() {
        val point = Point(10.5, 59.9)
        assertEquals(point, chronicleGson.fromJson(chronicleGson.toJson(point), Point::class.java))
    }

    @Test
    fun `coordinates are read even when they come before the type`() {
        val json = """{"coordinates":[10.5,59.9],"type":"Point"}"""
        assertEquals(Point(10.5, 59.9), chronicleGson.fromJson(json, Point::class.java))
    }

    @Test
    fun `a geometry declaring another type is rejected`() {
        val json = """{"type":"LineString","coordinates":[10.5,59.9]}"""
        assertThrows(JsonParseException::class.java) { chronicleGson.fromJson(json, Point::class.java) }
    }

    @Test
    fun `coordinates that are not a longitude latitude pair are rejected`() {
        val json = """{"type":"Point","coordinates":[10.5]}"""
        assertThrows(JsonParseException::class.java) { chronicleGson.fromJson(json, Point::class.java) }
    }

    @Test
    fun `a geometry without coordinates is rejected`() {
        assertThrows(JsonParseException::class.java) {
            chronicleGson.fromJson("""{"type":"Point"}""", Point::class.java)
        }
    }
}
