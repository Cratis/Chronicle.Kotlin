// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

@file:Suppress("DEPRECATION")

package io.cratis.chronicle.geospatial

import io.cratis.fundamentals.geospatial.Point as FundamentalsPoint

/**
 * Represents a geographic point with longitude and latitude.
 *
 * Longitude comes first, matching the GeoJSON coordinate order this type serializes to. Getting
 * that order the wrong way round is the single most common geospatial mistake, so the property
 * order deliberately mirrors the wire format rather than the "latitude, longitude" order used in
 * everyday speech.
 *
 * @property longitude The longitude of the point.
 * @property latitude The latitude of the point.
 */
@Deprecated(
    "Moved to io.cratis.fundamentals.geospatial.Point; same constructors and GeoJSON. Removed in the next major.",
    ReplaceWith("Point", "io.cratis.fundamentals.geospatial.Point")
)
data class Point(val longitude: Double, val latitude: Double) {

    /** Creates a point from its [io.cratis.fundamentals.geospatial.Point] equivalent. */
    constructor(value: FundamentalsPoint) : this(value.longitude, value.latitude)

    /** Converts this point to its [io.cratis.fundamentals.geospatial.Point] equivalent. */
    fun toFundamentals(): FundamentalsPoint = FundamentalsPoint(longitude, latitude)
}
