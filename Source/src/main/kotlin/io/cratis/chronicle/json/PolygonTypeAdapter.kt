// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.cratis.chronicle.geospatial.LinearRing
import io.cratis.chronicle.geospatial.Point
import io.cratis.chronicle.geospatial.Polygon
import kotlin.math.abs

/** Longitude and latitude are floating point, so ring closure is compared within a tolerance. */
private const val CLOSURE_TOLERANCE = 1e-9

/**
 * Serializes [Polygon] as a GeoJSON `Polygon`, which is what the kernel recognizes as a geospatial
 * value.
 *
 * GeoJSON gives a polygon one flat array of rings where the first is the shell and the rest are
 * holes, so the shell/holes split that makes [Polygon] readable only exists on this side of the wire.
 */
class PolygonTypeAdapter : TypeAdapter<Polygon>() {

    /** Writes [value] as `{ "type": "Polygon", "coordinates": [shell, ...holes] }`. */
    override fun write(out: JsonWriter, value: Polygon) =
        GeoJson.write(out, GeoJson.POLYGON) { coordinates ->
            coordinates.beginArray()
            GeoJson.writePoints(coordinates, value.shell.coordinates)
            value.holes.forEach { GeoJson.writePoints(coordinates, it.coordinates) }
            coordinates.endArray()
        }

    /** Reads a GeoJSON `Polygon` back into a [Polygon], taking the first ring as the shell. */
    override fun read(reader: JsonReader): Polygon =
        GeoJson.read(reader, GeoJson.POLYGON) { coordinates ->
            val rings = mutableListOf<LinearRing>()
            coordinates.beginArray()
            while (coordinates.hasNext()) {
                rings.add(readRing(coordinates))
            }
            coordinates.endArray()

            if (rings.isEmpty()) throw JsonParseException("A Polygon must have at least one ring (the shell)")
            Polygon(rings.first(), rings.drop(1))
        }

    private fun readRing(reader: JsonReader): LinearRing {
        val points = GeoJson.readPoints(reader)

        // A closed ring needs three distinct corners plus the repeat of the first one. Anything
        // less does not enclose an area, and the sink would reject the geometry on write.
        if (points.size < 4) throw JsonParseException("A LinearRing must have at least 4 points")
        if (!isClosed(points)) {
            throw JsonParseException("A LinearRing must be closed (first and last points must be identical)")
        }
        return LinearRing(points)
    }

    private fun isClosed(points: List<Point>): Boolean {
        val first = points.first()
        val last = points.last()
        return abs(first.longitude - last.longitude) <= CLOSURE_TOLERANCE &&
            abs(first.latitude - last.latitude) <= CLOSURE_TOLERANCE
    }
}
