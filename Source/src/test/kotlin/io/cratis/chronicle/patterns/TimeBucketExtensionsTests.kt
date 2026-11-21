// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.patterns

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The bucket must be derived with the rule the kernel mines with: a different rule asks about a slot the
 * mining never used and comes back empty, which looks exactly like "no established behavior".
 */
class TimeBucketExtensionsTests {

    private fun at(hour: Int, minute: Int = 0) = OffsetDateTime.of(2026, 1, 5, hour, minute, 0, 0, ZoneOffset.UTC)

    @Test
    fun `every hour boundary falls in the bucket the kernel uses`() {
        val expected = listOf(
            at(0) to TimeBucket.Night,
            at(4, 59) to TimeBucket.Night,
            at(5) to TimeBucket.EarlyMorning,
            at(7, 59) to TimeBucket.EarlyMorning,
            at(8) to TimeBucket.Morning,
            at(10, 59) to TimeBucket.Morning,
            at(11) to TimeBucket.Midday,
            at(13, 59) to TimeBucket.Midday,
            at(14) to TimeBucket.Afternoon,
            at(16, 59) to TimeBucket.Afternoon,
            at(17) to TimeBucket.Evening,
            at(21, 59) to TimeBucket.Evening,
            at(22) to TimeBucket.Night,
            at(23, 59) to TimeBucket.Night,
        )

        expected.forEach { (moment, bucket) -> assertEquals(bucket, moment.toTimeBucket(), "at $moment") }
    }

    @Test
    fun `the bucket is read from the moment's own offset and not from UTC`() {
        // Nine in the morning in Sydney is eleven at night the day before in UTC.
        val nineInSydney = OffsetDateTime.parse("2026-01-05T09:00+10:00")

        assertEquals(TimeBucket.Morning, nineInSydney.toTimeBucket())
        assertEquals(TimeBucket.Night, nineInSydney.withOffsetSameInstant(ZoneOffset.UTC).toTimeBucket())
    }

    @Test
    fun `a zoned moment is read in its own zone, daylight saving included`() {
        val summerEveningInOslo = ZonedDateTime.of(2026, 7, 1, 17, 30, 0, 0, ZoneId.of("Europe/Oslo"))

        assertEquals(TimeBucket.Evening, summerEveningInOslo.toTimeBucket())
    }
}
