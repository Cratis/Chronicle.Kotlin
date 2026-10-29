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

/** Java parity for the Fundamentals geospatial types used through the client. */
class JavaFundamentalsGeospatialTests {

    private val shell = FundamentalsLinearRing(
        FundamentalsPoint(0.0, 0.0), FundamentalsPoint(4.0, 0.0), FundamentalsPoint(4.0, 4.0), FundamentalsPoint(0.0, 0.0)
    )

    @Test
    fun `java constructs the Fundamentals types`() {
        assertEquals(10.5, JavaFundamentalsGeospatialUsage.point(10.5, 59.9).longitude)
        val line = JavaFundamentalsGeospatialUsage.lineString(FundamentalsPoint(1.0, 2.0), FundamentalsPoint(3.0, 4.0))
        assertEquals(FundamentalsLineString(listOf(FundamentalsPoint(1.0, 2.0), FundamentalsPoint(3.0, 4.0))), line)
        assertEquals(emptyList<FundamentalsLinearRing>(), JavaFundamentalsGeospatialUsage.polygon(shell).holes)
        assertEquals(listOf(shell), JavaFundamentalsGeospatialUsage.polygon(shell, listOf(shell)).holes)
    }

    @Test
    fun `java serializes and round-trips through the client's serializer`() {
        assertEquals(
            """{"type":"Point","coordinates":[10.5,59.9]}""",
            JavaFundamentalsGeospatialUsage.toJson(FundamentalsPoint(10.5, 59.9))
        )
        val polygon = FundamentalsPolygon(shell)
        val json = JavaFundamentalsGeospatialUsage.toJson(polygon)
        assertEquals(polygon, JavaFundamentalsGeospatialUsage.fromJson(json, FundamentalsPolygon::class.java))
    }

    @Test
    fun `java calls the conversions`() {
        assertEquals(FundamentalsPoint(1.0, 2.0), JavaFundamentalsGeospatialUsage.toFundamentals(Point(1.0, 2.0)))
        assertEquals(FundamentalsPolygon(shell), JavaFundamentalsGeospatialUsage.toLegacy(FundamentalsPolygon(shell)).toFundamentals())
    }
}
