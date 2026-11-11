// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * A reactor or reducer whose stream keeps ending waits longer each time, jittered so a herd spreads out, and starts
 * over once a stream has worked.
 */
class ObservationReconnectBackoffTests {
    private var now = 0L
    private var jitter = 1.0

    private fun backoff(baseDelay: Duration = 2.seconds) = ObservationReconnectBackoff(
        baseDelay = baseDelay,
        maxDelay = 60.seconds,
        healthyStreamDuration = 3.minutes,
        currentTimeMillis = { now },
        nextJitter = { jitter }
    )

    private fun ObservationReconnectBackoff.streamEnded(): Duration {
        streamOpened()
        return nextDelay()
    }

    @Test
    fun `doubles the wait for every stream that ends without working, up to the ceiling`() {
        val backoff = backoff()

        val delays = List(7) { backoff.streamEnded() }

        assertEquals(listOf(2, 4, 8, 16, 32, 60, 60).map { it.seconds }, delays)
    }

    @Test
    fun `starts over at the base delay once a stream delivered before it ended`() {
        val backoff = backoff()
        repeat(4) { backoff.streamEnded() }

        backoff.streamOpened()
        backoff.streamDelivered()
        val delay = backoff.nextDelay()

        assertEquals(2.seconds, delay)
    }

    @Test
    fun `starts over at the base delay once a stream stayed open long enough before it ended`() {
        val backoff = backoff()
        repeat(4) { backoff.streamEnded() }

        backoff.streamOpened()
        now += 3.minutes.inWholeMilliseconds
        val delay = backoff.nextDelay()

        assertEquals(2.seconds, delay)
    }

    @Test
    fun `keeps backing off when a stream ended before it stayed open long enough`() {
        val backoff = backoff()
        repeat(2) { backoff.streamEnded() }

        backoff.streamOpened()
        now += 2.minutes.inWholeMilliseconds
        val delay = backoff.nextDelay()

        assertEquals(8.seconds, delay)
    }

    @Test
    fun `lands every wait in the upper half of its ceiling`() {
        jitter = 0.0
        val lowest = backoff().streamEnded()
        jitter = 0.5
        val middle = backoff().streamEnded()
        jitter = 7.0
        val clamped = backoff().streamEnded()

        assertEquals(1.seconds, lowest)
        assertEquals(1_500.milliseconds, middle)
        assertEquals(2.seconds, clamped)
    }

    @Test
    fun `uses a base delay that is not positive as it is`() {
        val backoff = backoff(baseDelay = Duration.ZERO)

        val delays = List(3) { backoff.streamEnded() }

        assertEquals(List(3) { Duration.ZERO }, delays)
    }
}
