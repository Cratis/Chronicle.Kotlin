// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.cratis.chronicle.geospatial.LineString

/**
 * Serializes [LineString] as a GeoJSON `LineString`, which is what the kernel recognizes as a
 * geospatial value.
 */
class LineStringTypeAdapter : TypeAdapter<LineString>() {

    /** Writes [value] as `{ "type": "LineString", "coordinates": [[longitude, latitude], ...] }`. */
    override fun write(out: JsonWriter, value: LineString) =
        GeoJson.write(out, GeoJson.LINE_STRING) { GeoJson.writePoints(it, value.coordinates) }

    /** Reads a GeoJSON `LineString` back into a [LineString]. */
    override fun read(reader: JsonReader): LineString =
        GeoJson.read(reader, GeoJson.LINE_STRING) { coordinates ->
            val points = GeoJson.readPoints(coordinates)
            // A line needs two ends. Accepting fewer would hand the sink a geometry it rejects,
            // long after the append that produced it.
            if (points.size < 2) throw JsonParseException("A LineString must have at least 2 points")
            LineString(points)
        }
}
