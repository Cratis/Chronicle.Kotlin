// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * A [TypeAdapter] that puts a `java.time` value on the wire as a single ISO-8601 string.
 *
 * Gson has no `java.time` support of its own — left to reflection an [Instant] goes out as a
 * `{ seconds, nanos }` object (or fails outright on JDK 17 module rules) — while the schemas the client
 * registers describe every one of these types as a string with a `date-time`, `date-time-offset`, `date`,
 * or `time` format, so the kernel rejects the object. This adapter is what makes the JSON match the schema.
 *
 * It is `internal` and always wrapped in [nullSafe], so a null property is written as an omitted/`null`
 * value the same way it is for every other type.
 *
 * @param T The `java.time` type handled.
 * @property typeName The type name used in the message when a value cannot be read.
 * @property format Formats a value as the string that goes on the wire.
 * @property parse Parses the string that came off the wire.
 */
internal class JavaTimeTypeAdapter<T : Any>(
    private val typeName: String,
    private val format: (T) -> String,
    private val parse: (String) -> T
) : TypeAdapter<T>() {

    /** Writes [value] as a JSON string. */
    override fun write(out: JsonWriter, value: T) {
        out.value(format(value))
    }

    /** Reads a JSON string into the `java.time` value, rejecting anything that is not a valid ISO-8601 value. */
    override fun read(reader: JsonReader): T {
        val text = reader.nextString()
        try {
            return parse(text)
        } catch (exception: DateTimeParseException) {
            throw JsonParseException("'$text' is not a valid $typeName at ${reader.path}", exception)
        }
    }
}

/**
 * The `java.time` adapters registered on [chronicleGson], matching the formats the .NET client and the
 * kernel use for `DateTimeOffset`, `DateTime`, `DateOnly` and `TimeOnly`.
 *
 * Writes are always ISO-8601. Reads are lenient about how the offset is written, because .NET emits a UTC
 * `DateTimeOffset` as `2024-05-01T10:00:00+00:00` while the JVM writes `2024-05-01T10:00:00Z`; both are
 * accepted by every offset-carrying type.
 *
 * - [Instant] is written as ISO instant (`2024-05-01T10:00:00Z`) and read from any ISO offset date-time.
 * - [OffsetDateTime] is written and read as ISO offset date-time; the offset is preserved.
 * - [ZonedDateTime] is written as an offset date-time, so **the zone id is lost**: it is read back with the
 *   offset as its zone (`ZoneOffset`), which is the same instant but not the same zone rules. The schema
 *   format (`date-time-offset`) has no room for a zone id and the .NET client has no such type either.
 * - [LocalDateTime] is written without an offset (`2024-05-01T10:00:00`); an offset in incoming text is
 *   ignored, not converted.
 * - [LocalDate] is written as `2024-05-01` and [LocalTime] as `10:00:00` (with fractional seconds only when
 *   non-zero).
 */
internal object JavaTimeTypeAdapters {

    private const val OFFSET_DATE_TIME = "offset date-time"

    /** Every `java.time` class handled, paired with its null-safe adapter. */
    val all: List<Pair<Class<*>, TypeAdapter<*>>> = listOf(
        Instant::class.java to JavaTimeTypeAdapter<Instant>(
            "instant",
            { it.toString() },
            { OffsetDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant() }
        ).nullSafe(),
        OffsetDateTime::class.java to JavaTimeTypeAdapter<OffsetDateTime>(
            OFFSET_DATE_TIME,
            { DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(it) },
            { OffsetDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME) }
        ).nullSafe(),
        ZonedDateTime::class.java to JavaTimeTypeAdapter<ZonedDateTime>(
            "zoned date-time",
            { DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(it) },
            { ZonedDateTime.parse(it, DateTimeFormatter.ISO_ZONED_DATE_TIME) }
        ).nullSafe(),
        LocalDateTime::class.java to JavaTimeTypeAdapter<LocalDateTime>(
            "local date-time",
            { DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(it) },
            { LocalDateTime.parse(it, DateTimeFormatter.ISO_DATE_TIME) }
        ).nullSafe(),
        LocalDate::class.java to JavaTimeTypeAdapter<LocalDate>(
            "local date",
            { DateTimeFormatter.ISO_LOCAL_DATE.format(it) },
            { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }
        ).nullSafe(),
        LocalTime::class.java to JavaTimeTypeAdapter<LocalTime>(
            "local time",
            { DateTimeFormatter.ISO_LOCAL_TIME.format(it) },
            { LocalTime.parse(it, DateTimeFormatter.ISO_LOCAL_TIME) }
        ).nullSafe()
    )
}
