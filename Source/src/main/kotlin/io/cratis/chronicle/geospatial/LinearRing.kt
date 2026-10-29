// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

@file:Suppress("DEPRECATION")

package io.cratis.chronicle.geospatial

import io.cratis.fundamentals.geospatial.LinearRing as FundamentalsLinearRing

/**
 * Represents a closed linear ring, which forms a boundary of a [Polygon].
 *
 * @property coordinates The points that make up the linear ring. The first and last point must be
 *   identical so that the ring is closed.
 */
@Deprecated(
    "Moved to io.cratis.fundamentals.geospatial.LinearRing; same constructors and GeoJSON. Removed in the next major.",
    ReplaceWith("LinearRing", "io.cratis.fundamentals.geospatial.LinearRing")
)
data class LinearRing(val coordinates: List<Point>) {

    /**
     * Creates a linear ring from the given points.
     *
     * Java has no spread operator for list literals, so this vararg form keeps
     * `new LinearRing(a, b, c, a)` available without forcing callers to build a list first.
     *
     * @param coordinates The points that make up the linear ring.
     */
    constructor(vararg coordinates: Point) : this(coordinates.toList())

    /** Creates a LinearRing from its [io.cratis.fundamentals.geospatial.LinearRing] equivalent. */
    constructor(value: FundamentalsLinearRing) : this(value.coordinates.map { Point(it) })

    /** Converts this LinearRing to its [io.cratis.fundamentals.geospatial.LinearRing] equivalent. */
    fun toFundamentals(): FundamentalsLinearRing = FundamentalsLinearRing(coordinates.map { it.toFundamentals() })
}
