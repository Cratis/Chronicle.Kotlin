// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

@file:JvmName("TimeBucketExtensions")

package io.cratis.chronicle.patterns

import java.time.OffsetDateTime
import java.time.ZonedDateTime

/**
 * The [TimeBucket] this moment falls in, from the hour in the moment's own offset - not UTC. Somebody who
 * works at nine does so at nine in their morning.
 *
 * This is the rule the kernel mines patterns with; a query that derives its bucket any other way asks about
 * a slot the mining never used and comes back empty.
 */
fun OffsetDateTime.toTimeBucket(): TimeBucket = when (hour) {
    in 5 until 8 -> TimeBucket.EarlyMorning
    in 8 until 11 -> TimeBucket.Morning
    in 11 until 14 -> TimeBucket.Midday
    in 14 until 17 -> TimeBucket.Afternoon
    in 17 until 22 -> TimeBucket.Evening
    else -> TimeBucket.Night
}

/** The [TimeBucket] this moment falls in, from the hour in the moment's own zone - not UTC. */
fun ZonedDateTime.toTimeBucket(): TimeBucket = toOffsetDateTime().toTimeBucket()
