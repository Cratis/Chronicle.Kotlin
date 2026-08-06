// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.geospatial

/**
 * Represents a line string composed of two or more points.
 *
 * @property coordinates The points that make up the line string.
 */
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
}
