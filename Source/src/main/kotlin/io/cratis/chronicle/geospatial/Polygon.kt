// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.geospatial

/**
 * Represents a polygon with an outer shell and optional holes.
 *
 * The constructor is `@JvmOverloads` so that Java callers can write `new Polygon(shell)` for the
 * common hole-free case — Kotlin default arguments do not exist for Java.
 *
 * @property shell The outer boundary of the polygon.
 * @property holes Inner boundaries (holes) within the polygon. Empty when the polygon is solid.
 */
data class Polygon @JvmOverloads constructor(
    val shell: LinearRing,
    val holes: List<LinearRing> = emptyList()
)
