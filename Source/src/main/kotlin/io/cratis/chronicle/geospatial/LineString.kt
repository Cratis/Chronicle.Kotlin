// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

@file:Suppress("DEPRECATION")

package io.cratis.chronicle.geospatial

import io.cratis.fundamentals.geospatial.LineString as FundamentalsLineString

/**
 * Represents a line string composed of two or more points.
 *
 * @property coordinates The points that make up the line string.
 */
@Deprecated(
    "Moved to io.cratis.fundamentals.geospatial.LineString; same constructors and GeoJSON. Removed in the next major.",
    ReplaceWith("LineString", "io.cratis.fundamentals.geospatial.LineString")
)
data class LineString(val coordinates: List<Point>) {

    /**
     * Creates a line string from the given points.
     *
     * Java has no spread operator for list literals, so this vararg form keeps
     * `new LineString(a, b, c)` available without forcing callers to build a list first.
     *
     * @param coordinates The points that make up the line string.
     */
    constructor(vararg coordinates: Point) : this(coordinates.toList())

    /** Creates a LineString from its [io.cratis.fundamentals.geospatial.LineString] equivalent. */
    constructor(value: FundamentalsLineString) : this(value.coordinates.map { Point(it) })

    /** Converts this LineString to its [io.cratis.fundamentals.geospatial.LineString] equivalent. */
    fun toFundamentals(): FundamentalsLineString = FundamentalsLineString(coordinates.map { it.toFundamentals() })
}
