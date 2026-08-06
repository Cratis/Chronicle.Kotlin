// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.geospatial.LinearRing
import io.cratis.chronicle.geospatial.LineString
import io.cratis.chronicle.geospatial.Point
import io.cratis.chronicle.geospatial.Polygon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@EventType
private data class DeliveryArrived(val orderId: String, val at: Point)

private data class Territory(
    val name: String,
    val route: LineString,
    val area: Polygon,
    val lastKnownPosition: Point?
)

class ChronicleGsonTests {

    @Test
    fun `a geospatial property of an event serializes as GeoJSON rather than as a nested object`() {
        val json = chronicleGson.toJson(DeliveryArrived("order-1", Point(10.5, 59.9)))
        assertEquals(
            """{"orderId":"order-1","at":{"type":"Point","coordinates":[10.5,59.9]}}""",
            json
        )
    }

    @Test
    fun `every geospatial property of a read model round-trips`() {
        val territory = Territory(
            name = "Oslo",
            route = LineString(Point(10.5, 59.9), Point(10.7, 59.8)),
            area = Polygon(LinearRing(Point(0.0, 0.0), Point(4.0, 0.0), Point(4.0, 4.0), Point(0.0, 0.0))),
            lastKnownPosition = Point(10.6, 59.85)
        )
        val roundTripped = chronicleGson.fromJson(chronicleGson.toJson(territory), Territory::class.java)
        assertEquals(territory, roundTripped)
    }

    @Test
    fun `an absent geospatial property stays absent`() {
        val territory = Territory(
            name = "Oslo",
            route = LineString(Point(10.5, 59.9), Point(10.7, 59.8)),
            area = Polygon(LinearRing(Point(0.0, 0.0), Point(4.0, 0.0), Point(4.0, 4.0), Point(0.0, 0.0))),
            lastKnownPosition = null
        )
        val roundTripped = chronicleGson.fromJson(chronicleGson.toJson(territory), Territory::class.java)
        assertNull(roundTripped.lastKnownPosition)
    }
}
