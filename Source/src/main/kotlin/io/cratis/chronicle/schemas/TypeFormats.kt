// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.schemas

import io.cratis.chronicle.geospatial.LineString
import io.cratis.chronicle.geospatial.Point
import io.cratis.chronicle.geospatial.Polygon
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.reflect.KClass

/**
 * The `format` identifiers the kernel uses to recognize a schema property as a specific type rather
 * than as whatever JSON shape it happens to have.
 *
 * A geospatial value is the case that matters most: without a format the kernel sees an ordinary
 * nested object, and the sink cannot index or query it geospatially. The same reasoning applies to
 * every other entry here - a `UUID` or an `Instant` with no format is just a formatless `"string"`
 * leaf, and the sink cannot materialize it as a typed value. The identifiers are the same ones the
 * .NET client sends, so a read model written by one client is readable by the other.
 *
 * Two .NET entries are deliberately absent: `uint`/`uint32` and `ulong`/`uint64`. Kotlin's unsigned
 * types ([UInt], [ULong]) are `@JvmInline value class`es whose constructors and accessors carry
 * mangled JVM signatures - see `.ai/rules/kotlin.md` - so they are never put on the Java surface,
 * and there is no other JVM type that means "unsigned integer".
 */
internal object TypeFormats {

    private val formats = mapOf<KClass<*>, String>(
        Short::class to "int16",
        Int::class to "int32",
        Long::class to "int64",
        Float::class to "float",
        Double::class to "double",
        BigDecimal::class to "decimal",
        Byte::class to "byte",
        LocalDateTime::class to "date-time",
        OffsetDateTime::class to "date-time-offset",
        Instant::class to "date-time-offset",
        ZonedDateTime::class to "date-time-offset",
        LocalDate::class to "date",
        LocalTime::class to "time",
        Duration::class to "duration",
        UUID::class to "guid",
        Point::class to "point",
        LineString::class to "linestring",
        Polygon::class to "polygon",
        ByteArray::class to "byte-array"
    )

    /** The schema `format` for [type], or `null` when the type has no format of its own. */
    fun formatFor(type: KClass<*>): String? = formats[type]
}
