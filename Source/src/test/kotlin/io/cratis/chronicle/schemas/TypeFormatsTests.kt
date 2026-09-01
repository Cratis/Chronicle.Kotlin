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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TypeFormatsTests {

    @Test
    fun `formatFor maps every integer type to its kernel-recognized format`() {
        assertEquals("int16", TypeFormats.formatFor(Short::class))
        assertEquals("int32", TypeFormats.formatFor(Int::class))
        assertEquals("int64", TypeFormats.formatFor(Long::class))
        assertEquals("byte", TypeFormats.formatFor(Byte::class))
    }

    @Test
    fun `formatFor maps every floating point and decimal type to its kernel-recognized format`() {
        assertEquals("float", TypeFormats.formatFor(Float::class))
        assertEquals("double", TypeFormats.formatFor(Double::class))
        assertEquals("decimal", TypeFormats.formatFor(BigDecimal::class))
    }

    @Test
    fun `formatFor maps every date and time type to its kernel-recognized format`() {
        assertEquals("date-time", TypeFormats.formatFor(LocalDateTime::class))
        assertEquals("date-time-offset", TypeFormats.formatFor(OffsetDateTime::class))
        assertEquals("date-time-offset", TypeFormats.formatFor(Instant::class))
        assertEquals("date-time-offset", TypeFormats.formatFor(ZonedDateTime::class))
        assertEquals("date", TypeFormats.formatFor(LocalDate::class))
        assertEquals("time", TypeFormats.formatFor(LocalTime::class))
        assertEquals("duration", TypeFormats.formatFor(Duration::class))
    }

    @Test
    fun `formatFor maps UUID to the guid format`() {
        assertEquals("guid", TypeFormats.formatFor(UUID::class))
    }

    @Test
    fun `formatFor maps ByteArray to the byte-array format`() {
        assertEquals("byte-array", TypeFormats.formatFor(ByteArray::class))
    }

    @Test
    fun `formatFor maps every geospatial type to its kernel-recognized format`() {
        assertEquals("point", TypeFormats.formatFor(Point::class))
        assertEquals("linestring", TypeFormats.formatFor(LineString::class))
        assertEquals("polygon", TypeFormats.formatFor(Polygon::class))
    }

    @Test
    fun `formatFor returns null for a type with no known format`() {
        assertNull(TypeFormats.formatFor(String::class))
        assertNull(TypeFormats.formatFor(Boolean::class))
    }

    @Test
    fun `formatFor has no entry for unsigned types since they have no usable JVM equivalent`() {
        // UInt and ULong are @JvmInline value classes with mangled JVM signatures - never put on the
        // Java surface - and there is no other JVM type that means "unsigned integer", so the .NET
        // client's uint/uint32 and ulong/uint64 entries are deliberately not mirrored here.
        assertNull(TypeFormats.formatFor(UInt::class))
        assertNull(TypeFormats.formatFor(ULong::class))
    }
}
