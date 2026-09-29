// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import com.google.gson.JsonParseException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private data class Timeline(
    val instant: Instant? = null,
    val offsetDateTime: OffsetDateTime? = null,
    val zonedDateTime: ZonedDateTime? = null,
    val localDateTime: LocalDateTime? = null,
    val localDate: LocalDate? = null,
    val localTime: LocalTime? = null
)

class JavaTimeTypeAdapterTests {

    private val instant = Instant.parse("2024-05-01T10:15:30.123Z")

    private inline fun <reified T> roundTrip(value: T): T? =
        chronicleGson.fromJson(chronicleGson.toJson(value), T::class.java)

    @Test
    fun `an instant serializes as an ISO string rather than an object`() {
        assertEquals("\"2024-05-01T10:15:30.123Z\"", chronicleGson.toJson(instant))
    }

    @Test
    fun `an instant round-trips to millisecond precision`() {
        assertEquals(instant.toEpochMilli(), roundTrip(instant)!!.toEpochMilli())
    }

    @Test
    fun `an instant keeps nanosecond precision the JVM can carry`() {
        val precise = Instant.parse("2024-05-01T10:15:30.123456789Z")
        assertEquals(precise, roundTrip(precise))
    }

    @Test
    fun `an instant is read from a UTC offset the way dotnet writes it`() {
        assertEquals(instant, chronicleGson.fromJson("\"2024-05-01T10:15:30.1230000+00:00\"", Instant::class.java))
    }

    @Test
    fun `an instant is read from a non-zero offset as the same point in time`() {
        assertEquals(
            Instant.parse("2024-05-01T08:15:30Z"),
            chronicleGson.fromJson("\"2024-05-01T10:15:30+02:00\"", Instant::class.java)
        )
    }

    @Test
    fun `an instant is read from seven fractional digits`() {
        assertEquals(
            Instant.parse("2024-05-01T10:15:30.1234567Z"),
            chronicleGson.fromJson("\"2024-05-01T10:15:30.1234567Z\"", Instant::class.java)
        )
    }

    @Test
    fun `an offset date-time serializes as an ISO string and round-trips with its offset`() {
        val value = OffsetDateTime.of(2024, 5, 1, 10, 15, 30, 123_000_000, ZoneOffset.ofHours(2))
        assertEquals("\"2024-05-01T10:15:30.123+02:00\"", chronicleGson.toJson(value))
        assertEquals(value, roundTrip(value))
    }

    @Test
    fun `an offset date-time is read from a dotnet style UTC offset`() {
        val read = chronicleGson.fromJson("\"2024-05-01T10:15:30+00:00\"", OffsetDateTime::class.java)
        assertEquals(OffsetDateTime.of(2024, 5, 1, 10, 15, 30, 0, ZoneOffset.UTC), read)
    }

    @Test
    fun `a zoned date-time is written as an offset date-time`() {
        val value = ZonedDateTime.of(2024, 5, 1, 10, 15, 30, 0, ZoneId.of("Europe/Oslo"))
        assertEquals("\"2024-05-01T10:15:30+02:00\"", chronicleGson.toJson(value))
    }

    @Test
    fun `a zoned date-time round-trips to the same instant but loses its zone id`() {
        val value = ZonedDateTime.of(2024, 5, 1, 10, 15, 30, 0, ZoneId.of("Europe/Oslo"))
        val read = roundTrip(value)!!
        assertEquals(value.toInstant(), read.toInstant())
        assertEquals(ZoneOffset.ofHours(2), read.zone)
    }

    @Test
    fun `a local date-time serializes without an offset and round-trips`() {
        val value = LocalDateTime.of(2024, 5, 1, 10, 15, 30, 123_000_000)
        assertEquals("\"2024-05-01T10:15:30.123\"", chronicleGson.toJson(value))
        assertEquals(value, roundTrip(value))
    }

    @Test
    fun `a local date-time always carries seconds`() {
        assertEquals("\"2024-05-01T10:15:00\"", chronicleGson.toJson(LocalDateTime.of(2024, 5, 1, 10, 15)))
    }

    @Test
    fun `a local date-time ignores an offset in the incoming text`() {
        assertEquals(
            LocalDateTime.of(2024, 5, 1, 10, 15, 30),
            chronicleGson.fromJson("\"2024-05-01T10:15:30+02:00\"", LocalDateTime::class.java)
        )
    }

    @Test
    fun `a local date serializes as a date and round-trips`() {
        val value = LocalDate.of(2024, 5, 1)
        assertEquals("\"2024-05-01\"", chronicleGson.toJson(value))
        assertEquals(value, roundTrip(value))
    }

    @Test
    fun `a local time serializes with seconds and round-trips`() {
        assertEquals("\"10:15:00\"", chronicleGson.toJson(LocalTime.of(10, 15)))
        val value = LocalTime.of(10, 15, 30, 123_000_000)
        assertEquals(value, roundTrip(value))
    }

    @Test
    fun `a local time is read from the seven digit format dotnet writes`() {
        assertEquals(
            LocalTime.of(10, 15, 30),
            chronicleGson.fromJson("\"10:15:30.0000000\"", LocalTime::class.java)
        )
    }

    @Test
    fun `a null value is written as null and read as null for every type`() {
        assertEquals("null", chronicleGson.toJson(null as Instant?, Instant::class.java))
        assertNull(chronicleGson.fromJson("null", Instant::class.java))
        assertNull(chronicleGson.fromJson("null", OffsetDateTime::class.java))
        assertNull(chronicleGson.fromJson("null", ZonedDateTime::class.java))
        assertNull(chronicleGson.fromJson("null", LocalDateTime::class.java))
        assertNull(chronicleGson.fromJson("null", LocalDate::class.java))
        assertNull(chronicleGson.fromJson("null", LocalTime::class.java))
    }

    @Test
    fun `absent and null properties stay absent`() {
        assertEquals("{}", chronicleGson.toJson(Timeline()))
        assertEquals(Timeline(), chronicleGson.fromJson("""{"instant":null,"localDate":null}""", Timeline::class.java))
    }

    @Test
    fun `every java time property of an object serializes as a string and round-trips`() {
        val timeline = Timeline(
            instant = instant,
            offsetDateTime = OffsetDateTime.of(2024, 5, 1, 10, 15, 30, 0, ZoneOffset.ofHours(-5)),
            zonedDateTime = ZonedDateTime.of(2024, 5, 1, 10, 15, 30, 0, ZoneOffset.UTC),
            localDateTime = LocalDateTime.of(2024, 5, 1, 10, 15, 30),
            localDate = LocalDate.of(2024, 5, 1),
            localTime = LocalTime.of(10, 15, 30)
        )
        val json = chronicleGson.toJson(timeline)
        assertEquals(
            """{"instant":"2024-05-01T10:15:30.123Z","offsetDateTime":"2024-05-01T10:15:30-05:00",""" +
                """"zonedDateTime":"2024-05-01T10:15:30Z","localDateTime":"2024-05-01T10:15:30",""" +
                """"localDate":"2024-05-01","localTime":"10:15:30"}""",
            json
        )
        assertEquals(timeline, chronicleGson.fromJson(json, Timeline::class.java))
    }

    @Test
    fun `text that is not a valid value is rejected with a parse exception naming the type`() {
        val exception = assertThrows(JsonParseException::class.java) {
            chronicleGson.fromJson("\"yesterday\"", Instant::class.java)
        }
        assertTrue(exception.message!!.contains("yesterday"))
        assertTrue(exception.message!!.contains("instant"))
    }

    @Test
    fun `an object where a string is expected is rejected`() {
        assertThrows(JsonParseException::class.java) {
            chronicleGson.fromJson("""{"seconds":1,"nanos":0}""", Instant::class.java)
        }
    }
}
