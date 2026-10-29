// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

@file:Suppress("DEPRECATION")

package io.cratis.chronicle.geospatial

import io.cratis.fundamentals.geospatial.Polygon as FundamentalsPolygon

/**
 * Represents a polygon with an outer shell and optional holes.
 *
 * The constructor is `@JvmOverloads` so that Java callers can write `new Polygon(shell)` for the
 * common hole-free case — Kotlin default arguments do not exist for Java.
 *
 * @property shell The outer boundary of the polygon.
 * @property holes Inner boundaries (holes) within the polygon. Empty when the polygon is solid.
 */
@Deprecated(
    "Moved to io.cratis.fundamentals.geospatial.Polygon; same constructors and GeoJSON. Removed in the next major.",
    ReplaceWith("Polygon", "io.cratis.fundamentals.geospatial.Polygon")
)
data class Polygon @JvmOverloads constructor(
    val shell: LinearRing,
    val holes: List<LinearRing> = emptyList()
) {

    /** Creates a polygon from its [io.cratis.fundamentals.geospatial.Polygon] equivalent. */
    constructor(value: FundamentalsPolygon) : this(LinearRing(value.shell), value.holes.map { LinearRing(it) })

    /** Converts this polygon to its [io.cratis.fundamentals.geospatial.Polygon] equivalent. */
    fun toFundamentals(): FundamentalsPolygon = FundamentalsPolygon(shell.toFundamentals(), holes.map { it.toFundamentals() })
}
