// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

@file:JvmName("ChronicleGson")

package io.cratis.chronicle.json

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.cratis.chronicle.geospatial.LineString
import io.cratis.chronicle.geospatial.Point
import io.cratis.chronicle.geospatial.Polygon

/**
 * The [Gson] instance the client serializes event content and read model state with.
 *
 * Everything the client sends to, or receives from, the kernel goes through this one instance so
 * that a type serializes identically no matter which path it takes — appending an event, seeding,
 * a reactor receiving it back, or a read model being materialized. The geospatial adapters are the
 * reason it exists: the kernel identifies geospatial values by their GeoJSON shape, which plain
 * reflection over [Point], [LineString], and [Polygon] would not produce.
 *
 * It is public so that callers who serialize event content themselves produce the same JSON the
 * client would. Java reaches it as `ChronicleGson.chronicleGson`.
 */
@JvmField
val chronicleGson: Gson = GsonBuilder()
    .registerTypeAdapter(Point::class.java, PointTypeAdapter().nullSafe())
    .registerTypeAdapter(LineString::class.java, LineStringTypeAdapter().nullSafe())
    .registerTypeAdapter(Polygon::class.java, PolygonTypeAdapter().nullSafe())
    .create()
