// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.json

import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.math.BigInteger
import java.time.Duration
import java.time.format.DateTimeParseException

/**
 * Writes durations in .NET's constant TimeSpan format, with optional days and seven-digit tick fractions.
 * Nanoseconds below a 100 ns tick are truncated toward zero. Tick magnitudes above [Long.MAX_VALUE]
 * are rejected rather than emitting a value the kernel cannot parse. Reads also accept common invariant
 * TimeSpan forms and ISO-8601 durations. Registered with [nullSafe] on [chronicleGson].
 */
internal class DurationTypeAdapter : TypeAdapter<Duration>() {
    private val ticksPerSecond = BigInteger.valueOf(10_000_000)
    private val maximumTicks = BigInteger.valueOf(Long.MAX_VALUE)
    // TimeSpan holds at most 10,675,199 days, so a days part never needs more than eight digits. Bounding it keeps
    // an oversized input from costing more than a regular parse before it is rejected.
    private val timePattern = Regex("""(-)?(?:(\d{1,8})\.)?(\d{1,2}):(\d{1,2})(?::(\d{1,2})(?:\.(\d{1,7}))?)?""")
    private val daysPattern = Regex("""-?\d{1,8}""")

    override fun write(out: JsonWriter, value: Duration) {
        val ticks = ticks(value)
        val magnitude = kotlin.math.abs(ticks)
        val seconds = magnitude / 10_000_000
        val days = seconds / 86_400
        val fraction = magnitude % 10_000_000
        out.value(buildString {
            if (ticks < 0) append('-')
            if (days > 0) append(days).append('.')
            append((seconds / 3_600 % 24).toString().padStart(2, '0'))
            append(':').append((seconds / 60 % 60).toString().padStart(2, '0'))
            append(':').append((seconds % 60).toString().padStart(2, '0'))
            if (fraction != 0L) append('.').append(fraction.toString().padStart(7, '0'))
        })
    }

    override fun read(reader: JsonReader): Duration {
        val text = readDurationText(reader)
        try {
            val duration = parseDuration(text)
            ticks(duration)
            return duration
        } catch (exception: DateTimeParseException) {
            throw JsonParseException("'$text' is not a valid ISO-8601 duration at ${reader.path}", exception)
        } catch (exception: IllegalArgumentException) {
            throw JsonParseException("'$text' is not a valid TimeSpan duration at ${reader.path}", exception)
        } catch (exception: ArithmeticException) {
            throw JsonParseException("'$text' is outside the TimeSpan range at ${reader.path}", exception)
        }
    }

    private fun readDurationText(reader: JsonReader): String {
        if (reader.peek() != JsonToken.STRING) {
            throw JsonParseException("Expected a duration string at ${reader.path}")
        }
        return reader.nextString().trim()
    }

    private fun parseDuration(text: String): Duration =
        if (text.trimStart('-', '+').startsWith("P", ignoreCase = true)) Duration.parse(text) else parseTimeSpan(text)

    private fun parseTimeSpan(text: String): Duration {
        if (daysPattern.matches(text)) return fromTicks(text.toBigInteger() * BigInteger.valueOf(86_400) * ticksPerSecond)
        val match = requireNotNull(timePattern.matchEntire(text)) { "Expected invariant TimeSpan format" }
        val (sign, days, hours, minutes, seconds, fraction) = match.destructured
        require(hours.toInt() <= 23 && minutes.toInt() <= 59 && (seconds.ifEmpty { "0" }).toInt() <= 59) {
            "TimeSpan hours, minutes or seconds are out of range"
        }
        val totalSeconds = (days.ifEmpty { "0" }.toBigInteger() * BigInteger.valueOf(24) + hours.toBigInteger()) *
            BigInteger.valueOf(3_600) + minutes.toBigInteger() * BigInteger.valueOf(60) + seconds.ifEmpty { "0" }.toBigInteger()
        val magnitude = totalSeconds * ticksPerSecond + fraction.padEnd(7, '0').toBigInteger()
        return fromTicks(if (sign == "-") magnitude.negate() else magnitude)
    }

    private fun fromTicks(value: BigInteger): Duration {
        requireInRange(value)
        val (seconds, remainder) = value.divideAndRemainder(ticksPerSecond)
        return Duration.ofSeconds(seconds.toLong(), remainder.toLong() * 100)
    }

    private fun ticks(value: Duration): Long {
        val nanos = BigInteger.valueOf(value.seconds) * BigInteger.valueOf(1_000_000_000) + BigInteger.valueOf(value.nano.toLong())
        val ticks = nanos / BigInteger.valueOf(100)
        requireInRange(ticks)
        return ticks.toLong()
    }

    private fun requireInRange(ticks: BigInteger) {
        require(ticks.abs() <= maximumTicks) { "Duration is outside the TimeSpan range (maximum tick magnitude is ${Long.MAX_VALUE})" }
    }
}
