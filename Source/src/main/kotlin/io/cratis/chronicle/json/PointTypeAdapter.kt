// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.cratis.chronicle.geospatial.Point

/**
 * Serializes [Point] as a GeoJSON `Point`, which is what the kernel recognizes as a geospatial
 * value. Without this the point would go over the wire as an anonymous `{ longitude, latitude }`
 * object and the sink could not index or query it geospatially.
 */
class PointTypeAdapter : TypeAdapter<Point>() {

    /** Writes [value] as `{ "type": "Point", "coordinates": [longitude, latitude] }`. */
    override fun write(out: JsonWriter, value: Point) =
        GeoJson.write(out, GeoJson.POINT) { GeoJson.writePoint(it, value) }

    /** Reads a GeoJSON `Point` back into a [Point]. */
    override fun read(reader: JsonReader): Point =
        GeoJson.read(reader, GeoJson.POINT) { GeoJson.readPoint(it) }
}
