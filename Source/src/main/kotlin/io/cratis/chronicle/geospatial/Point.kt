// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.geospatial

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
data class Point(val longitude: Double, val latitude: Double)
