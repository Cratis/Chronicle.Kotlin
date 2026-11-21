// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.patterns

/**
 * The part of the day an action happened in, read from the moment's own offset.
 *
 * Derive it with [toTimeBucket] - the same rule the kernel mines with - rather than computing it yourself.
 */
enum class TimeBucket {
    /** 05:00 up to 08:00. */
    EarlyMorning,

    /** 08:00 up to 11:00. */
    Morning,

    /** 11:00 up to 14:00. */
    Midday,

    /** 14:00 up to 17:00. */
    Afternoon,

    /** 17:00 up to 22:00. */
    Evening,

    /** 22:00 up to 05:00. */
    Night,
}
