// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import com.google.gson.JsonParseException
import java.time.Duration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DurationTypeAdapterTests {
    private fun read(text: String): Duration = chronicleGson.fromJson("\"$text\"", Duration::class.java)

    private fun assertWire(value: Duration, text: String) {
        assertEquals("\"$text\"", chronicleGson.toJson(value))
        assertEquals(value, read(text))
    }

    @Test
    fun `an hour is written in constant TimeSpan format and round-trips`() {
        assertWire(Duration.ofHours(1), "01:00:00")
    }

    @Test
    fun `days and fractional seconds are written in constant TimeSpan format and round-trip`() {
        assertWire(Duration.ofDays(1).plusHours(2).plusMinutes(3).plusSeconds(4).plusMillis(500), "1.02:03:04.5000000")
    }

    @Test
    fun `negative durations have a single leading sign and round-trip`() {
        assertWire(Duration.ofMillis(-1500), "-00:00:01.5000000")
        assertWire(Duration.ofDays(-2).minusHours(3), "-2.03:00:00")
    }

    @Test
    fun `zero has neither a day part nor a fraction`() {
        assertWire(Duration.ZERO, "00:00:00")
    }

    @Test
    fun `nanoseconds are truncated toward zero to ticks`() {
        assertEquals("\"00:00:00.0000001\"", chronicleGson.toJson(Duration.ofNanos(123)))
        assertEquals("\"-00:00:00.0000001\"", chronicleGson.toJson(Duration.ofNanos(-123)))
        assertEquals(Duration.ofNanos(100), read("00:00:00.0000001"))
        assertEquals("\"00:00:00\"", chronicleGson.toJson(Duration.ofNanos(-99)))
        assertEquals("\"00:00:00\"", chronicleGson.toJson(Duration.ofNanos(99)))
    }

    @Test
    fun `common invariant TimeSpan forms are read leniently`() {
        assertEquals(Duration.ofDays(1).plusHours(2).plusMinutes(3).plusSeconds(4).plusMillis(500), read("1.02:03:04.5"))
        assertEquals(Duration.ofSeconds(-1), read("-00:00:01"))
        assertEquals(Duration.ofHours(2).plusMinutes(3), read("2:03"))
        assertEquals(Duration.ofDays(1).plusHours(2).plusMinutes(3), read("1.2:03"))
        assertEquals(Duration.ofSeconds(3723).plusNanos(123456700), read("1:2:3.1234567"))
        assertEquals(Duration.ofDays(12), read("12"))
        assertEquals(Duration.ofDays(-12), read("-12"))
    }

    @Test
    fun `ISO durations remain readable`() {
        assertEquals(Duration.ofMinutes(90), read("PT1H30M"))
        assertEquals(Duration.ofDays(2), read("P2D"))
        assertEquals(Duration.ofMillis(-1500), read("-PT1.5S"))
    }

    @Test
    fun `the largest supported tick magnitudes round-trip`() {
        val maximum = Duration.ofSeconds(Long.MAX_VALUE / 10_000_000, Long.MAX_VALUE % 10_000_000 * 100)
        assertWire(maximum, "10675199.02:48:05.4775807")
        assertWire(maximum.negated(), "-10675199.02:48:05.4775807")
    }

    @Test
    fun `values outside the TimeSpan tick range cannot be written`() {
        val maximum = Duration.ofSeconds(Long.MAX_VALUE / 10_000_000, Long.MAX_VALUE % 10_000_000 * 100)
        listOf(maximum.plusNanos(100), maximum.negated().minusNanos(100), Duration.ofSeconds(Long.MAX_VALUE)).forEach {
            val exception = assertThrows(IllegalArgumentException::class.java) { chronicleGson.toJson(it) }
            assertTrue(exception.message!!.contains("TimeSpan"))
        }
    }

    @Test
    fun `invalid or overflowing duration text is rejected`() {
        listOf("yesterday", "24:00:00", "00:60:00", "00:00:60", "00:00:00.12345678", "1.2", "10675200", "P999999999999999999999D").forEach {
            assertThrows(JsonParseException::class.java) { read(it) }
        }
    }

    @Test
    fun `JSON numbers and other non-string values are rejected`() {
        listOf("1", "1.5", "true", "[]", "{}").forEach {
            assertThrows(JsonParseException::class.java) { chronicleGson.fromJson(it, Duration::class.java) }
        }
    }

    @Test
    fun `null durations are written and read as null`() {
        assertEquals("null", chronicleGson.toJson(null, Duration::class.java))
        assertNull(chronicleGson.fromJson("null", Duration::class.java))
    }
}
