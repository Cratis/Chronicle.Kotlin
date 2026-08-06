// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.geospatial

/**
 * Represents a closed linear ring, which forms a boundary of a [Polygon].
 *
 * @property coordinates The points that make up the linear ring. The first and last point must be
 *   identical so that the ring is closed.
 */
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
}
