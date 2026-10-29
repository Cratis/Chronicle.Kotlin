// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

@file:Suppress("DEPRECATION")

package io.cratis.chronicle.geospatial

import io.cratis.fundamentals.geospatial.LineString as FundamentalsLineString
import io.cratis.fundamentals.geospatial.LinearRing as FundamentalsLinearRing
import io.cratis.fundamentals.geospatial.Point as FundamentalsPoint
import io.cratis.fundamentals.geospatial.Polygon as FundamentalsPolygon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GeospatialConversionTests {

    private val shell = LinearRing(Point(0.0, 0.0), Point(4.0, 0.0), Point(4.0, 4.0), Point(0.0, 0.0))
    private val hole = LinearRing(Point(1.0, 1.0), Point(2.0, 1.0), Point(2.0, 2.0), Point(1.0, 1.0))

    @Test
    fun `a point converts both ways`() {
        val point = Point(10.5, 59.9)
        assertEquals(FundamentalsPoint(10.5, 59.9), point.toFundamentals())
        assertEquals(point, Point(point.toFundamentals()))
    }

    @Test
    fun `a line string converts both ways`() {
        val line = LineString(Point(1.0, 2.0), Point(3.0, 4.0))
        assertEquals(FundamentalsLineString(FundamentalsPoint(1.0, 2.0), FundamentalsPoint(3.0, 4.0)), line.toFundamentals())
        assertEquals(line, LineString(line.toFundamentals()))
    }

    @Test
    fun `a linear ring converts both ways`() {
        assertEquals(shell, LinearRing(shell.toFundamentals()))
        assertEquals(FundamentalsPoint(4.0, 4.0), shell.toFundamentals().coordinates[2])
        assertEquals(FundamentalsLinearRing::class, shell.toFundamentals()::class)
    }

    @Test
    fun `a polygon with holes converts both ways`() {
        val polygon = Polygon(shell, listOf(hole))
        val converted: FundamentalsPolygon = polygon.toFundamentals()
        assertEquals(shell.toFundamentals(), converted.shell)
        assertEquals(listOf(hole.toFundamentals()), converted.holes)
        assertEquals(polygon, Polygon(converted))
    }

    @Test
    fun `an empty line string is not ambiguous`() {
        assertEquals(emptyList<Point>(), LineString(emptyList()).coordinates)
    }
}
