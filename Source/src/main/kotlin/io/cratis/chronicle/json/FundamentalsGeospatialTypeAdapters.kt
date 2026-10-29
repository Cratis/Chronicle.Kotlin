// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

@file:Suppress("DEPRECATION")

package io.cratis.chronicle.json

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.cratis.chronicle.geospatial.LineString
import io.cratis.chronicle.geospatial.Point
import io.cratis.chronicle.geospatial.Polygon
import io.cratis.fundamentals.geospatial.LineString as FundamentalsLineString
import io.cratis.fundamentals.geospatial.Point as FundamentalsPoint
import io.cratis.fundamentals.geospatial.Polygon as FundamentalsPolygon

/**
 * Writes and reads the Fundamentals geospatial types by delegating to the existing adapters, so the
 * GeoJSON wire format and validation are identical to the Chronicle types'.
 */
internal class FundamentalsPointTypeAdapter : TypeAdapter<FundamentalsPoint>() {
    private val delegate = PointTypeAdapter()

    override fun write(out: JsonWriter, value: FundamentalsPoint) = delegate.write(out, Point(value))

    override fun read(reader: JsonReader): FundamentalsPoint = delegate.read(reader).toFundamentals()
}

/** See [FundamentalsPointTypeAdapter]. */
internal class FundamentalsLineStringTypeAdapter : TypeAdapter<FundamentalsLineString>() {
    private val delegate = LineStringTypeAdapter()

    override fun write(out: JsonWriter, value: FundamentalsLineString) = delegate.write(out, LineString(value))

    override fun read(reader: JsonReader): FundamentalsLineString = delegate.read(reader).toFundamentals()
}

/** See [FundamentalsPointTypeAdapter]. */
internal class FundamentalsPolygonTypeAdapter : TypeAdapter<FundamentalsPolygon>() {
    private val delegate = PolygonTypeAdapter()

    override fun write(out: JsonWriter, value: FundamentalsPolygon) = delegate.write(out, Polygon(value))

    override fun read(reader: JsonReader): FundamentalsPolygon = delegate.read(reader).toFundamentals()
}
