// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.geospatial

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Java parity for the geospatial types.
 *
 * [JavaGeospatialUsage] is the part that matters: it fails to compile if a constructor stops being
 * callable from Java — which is what happens when a parameter with a Kotlin default argument loses
 * its `@JvmOverloads`, or when a vararg form is dropped. These assertions then confirm the values
 * land where expected and serialize identically to the Kotlin side.
 */
class JavaGeospatialTests {

    private val shell = LinearRing(Point(0.0, 0.0), Point(4.0, 0.0), Point(4.0, 4.0), Point(0.0, 0.0))

    @Test
    fun `java constructs a point with longitude first`() {
        val point = JavaGeospatialUsage.point(10.5, 59.9)
        assertEquals(10.5, point.longitude)
        assertEquals(59.9, point.latitude)
    }

    @Test
    fun `java builds the same line string from a list and from loose points`() {
        val first = Point(10.5, 59.9)
        val second = Point(10.7, 59.8)
        assertEquals(
            JavaGeospatialUsage.lineStringFromList(listOf(first, second)),
            JavaGeospatialUsage.lineStringFromPoints(first, second)
        )
    }

    @Test
    fun `java constructs a polygon without naming the holes`() {
        val polygon = JavaGeospatialUsage.polygonWithoutHoles(shell)
        assertEquals(shell, polygon.shell)
        assertEquals(emptyList<LinearRing>(), polygon.holes)
    }

    @Test
    fun `java constructs a polygon with holes`() {
        val hole = JavaGeospatialUsage.ring(Point(1.0, 1.0), Point(2.0, 1.0), Point(2.0, 2.0), Point(1.0, 1.0))
        val polygon = JavaGeospatialUsage.polygonWithHoles(shell, listOf(hole))
        assertEquals(listOf(hole), polygon.holes)
    }

    @Test
    fun `java serializes a point to the same GeoJSON kotlin does`() {
        assertEquals(
            """{"type":"Point","coordinates":[10.5,59.9]}""",
            JavaGeospatialUsage.toJson(Point(10.5, 59.9))
        )
    }

    @Test
    fun `java round-trips a polygon through the client's own serializer`() {
        val polygon = Polygon(shell)
        val json = JavaGeospatialUsage.toJson(polygon)
        assertEquals(polygon, JavaGeospatialUsage.fromJson(json, Polygon::class.java))
    }
}
