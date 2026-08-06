// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import com.google.gson.JsonParseException
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.cratis.chronicle.geospatial.Point

/**
 * The [GeoJSON](https://geojson.org) wire format shared by the geospatial type adapters.
 *
 * Every GeoJSON geometry is a `{ "type": ..., "coordinates": ... }` object whose coordinates are
 * nested `[longitude, latitude]` pairs. Only the nesting depth differs per geometry, so the envelope
 * and the coordinate pair live here once instead of being repeated in each adapter.
 */
internal object GeoJson {

    /** The `type` discriminator for a [io.cratis.chronicle.geospatial.Point]. */
    const val POINT = "Point"

    /** The `type` discriminator for a [io.cratis.chronicle.geospatial.LineString]. */
    const val LINE_STRING = "LineString"

    /** The `type` discriminator for a [io.cratis.chronicle.geospatial.Polygon]. */
    const val POLYGON = "Polygon"

    /** Writes the `{ "type": ..., "coordinates": ... }` envelope, delegating the coordinates to [coordinates]. */
    fun write(out: JsonWriter, type: String, coordinates: (JsonWriter) -> Unit) {
        out.beginObject()
        out.name("type").value(type)
        out.name("coordinates")
        coordinates(out)
        out.endObject()
    }

    /**
     * Reads the `{ "type": ..., "coordinates": ... }` envelope, delegating the coordinates to
     * [coordinates], and fails unless the geometry declared itself as [type].
     *
     * The two members are accepted in either order because GeoJSON does not mandate one, so the
     * declared type is only validated once the whole object has been read.
     */
    fun <T : Any> read(reader: JsonReader, type: String, coordinates: (JsonReader) -> T): T {
        var declaredType: String? = null
        var result: T? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName().lowercase()) {
                "type" -> declaredType = reader.nextString()
                "coordinates" -> result = coordinates(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        if (declaredType != type) throw JsonParseException("Expected type to be '$type', but was '$declaredType'")
        return result ?: throw JsonParseException("Expected a coordinates member on the $type")
    }

    /** Writes a single point as the `[longitude, latitude]` pair GeoJSON expects. */
    fun writePoint(out: JsonWriter, point: Point) {
        out.beginArray()
        out.value(point.longitude)
        out.value(point.latitude)
        out.endArray()
    }

    /** Reads a single `[longitude, latitude]` pair. */
    fun readPoint(reader: JsonReader): Point {
        val coordinates = mutableListOf<Double>()
        reader.beginArray()
        while (reader.hasNext()) {
            coordinates.add(reader.nextDouble())
        }
        reader.endArray()

        if (coordinates.size != 2) {
            throw JsonParseException("Each coordinate must have exactly 2 elements [longitude, latitude]")
        }
        return Point(coordinates[0], coordinates[1])
    }

    /** Writes a sequence of points as an array of `[longitude, latitude]` pairs. */
    fun writePoints(out: JsonWriter, points: List<Point>) {
        out.beginArray()
        points.forEach { writePoint(out, it) }
        out.endArray()
    }

    /** Reads an array of `[longitude, latitude]` pairs. */
    fun readPoints(reader: JsonReader): List<Point> {
        val points = mutableListOf<Point>()
        reader.beginArray()
        while (reader.hasNext()) {
            points.add(readPoint(reader))
        }
        reader.endArray()
        return points
    }
}
