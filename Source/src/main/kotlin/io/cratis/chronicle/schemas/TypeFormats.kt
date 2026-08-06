// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.schemas

import io.cratis.chronicle.geospatial.LineString
import io.cratis.chronicle.geospatial.Point
import io.cratis.chronicle.geospatial.Polygon
import kotlin.reflect.KClass

/**
 * The `format` identifiers the kernel uses to recognize a schema property as a specific type rather
 * than as whatever JSON shape it happens to have.
 *
 * A geospatial value is the case that matters: without a format the kernel sees an ordinary nested
 * object, and the sink cannot index or query it geospatially. The identifiers are the same ones the
 * .NET client sends, so a read model written by one client is readable by the other.
 */
internal object TypeFormats {

    private val formats = mapOf<KClass<*>, String>(
        Point::class to "point",
        LineString::class to "linestring",
        Polygon::class to "polygon"
    )

    /** The schema `format` for [type], or `null` when the type has no format of its own. */
    fun formatFor(type: KClass<*>): String? = formats[type]
}
