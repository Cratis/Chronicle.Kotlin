// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Decides how long one reactor or reducer waits before opening its observation stream again after it ended.
 *
 * A fixed delay is right for one observer and wrong for many: seventy reactors whose streams fail together come
 * back together, every two seconds, against a kernel that is still busy - a thundering herd by construction. So each
 * stream that ends without having worked doubles the wait, up to [maxDelay], and every wait is jittered into the upper
 * half of its ceiling so a herd spreads out.
 *
 * The wait starts over once a stream has worked: it delivered something, or it stayed open for [healthyStreamDuration].
 * A stream the kernel closes after a long life therefore comes back after [baseDelay], not a backed-off one. A base
 * delay of zero or less is used as it is.
 *
 * @param baseDelay The wait after a stream that had worked.
 * @param maxDelay The longest wait, however often streams end.
 * @param healthyStreamDuration How long a stream must stay open to count as having worked.
 * @param currentTimeMillis A monotonic clock, in milliseconds, measuring how long a stream stayed open.
 * @param nextJitter Returns a value in [0, 1) choosing where in the upper half of the wait this one lands.
 */
internal class ObservationReconnectBackoff(
    private val baseDelay: Duration = DEFAULT_BASE_DELAY,
    private val maxDelay: Duration = DEFAULT_MAX_DELAY,
    private val healthyStreamDuration: Duration = DEFAULT_HEALTHY_STREAM_DURATION,
    private val currentTimeMillis: () -> Long = { System.nanoTime() / NANOS_PER_MILLI },
    private val nextJitter: () -> Double = Random::nextDouble
) {
    private val streamsEndedWithoutWorking = AtomicInteger(0)
    private val streamOpenedAt = AtomicLong(currentTimeMillis())

    /** Records that a new observation stream was opened. */
    fun streamOpened() = streamOpenedAt.set(currentTimeMillis())

    /** Records that the open stream delivered something, which proves it was established. */
    fun streamDelivered() = streamsEndedWithoutWorking.set(0)

    /** Gets how long to wait before opening the stream again, now that the open one has ended. */
    fun nextDelay(): Duration {
        if (baseDelay <= Duration.ZERO) return baseDelay

        if ((currentTimeMillis() - streamOpenedAt.get()).milliseconds >= healthyStreamDuration) {
            streamsEndedWithoutWorking.set(0)
        }

        val exponent = minOf(streamsEndedWithoutWorking.incrementAndGet() - 1, MAX_EXPONENT)
        val ceiling = minOf(maxDelay.inWholeMilliseconds.toDouble(), baseDelay.inWholeMilliseconds * 2.0.pow(exponent))
        val jitter = 0.5 + 0.5 * nextJitter().coerceIn(0.0, 1.0)
        return (ceiling * jitter).toLong().milliseconds
    }

    companion object {
        /**
         * The default wait before re-establishing an observation whose stream ended, once the stream had worked.
         * Streams that end without having worked back off from this.
         */
        val DEFAULT_BASE_DELAY: Duration = 2.seconds

        /** The default longest wait between attempts. */
        val DEFAULT_MAX_DELAY: Duration = 60.seconds

        /**
         * The default time a stream must stay open to count as having worked.
         *
         * It must be longer than the kernel takes to give up establishing a stream: the kernel asks a busy observer
         * again on the same stream, up to four attempts of a thirty-second grain call each, before it fails the stream.
         * A shorter threshold would count every such stream as having worked and start the wait over at the base delay -
         * exactly for the herd of slow subscribes the backoff exists for. A stream that has delivered something counts
         * as having worked at once.
         */
        val DEFAULT_HEALTHY_STREAM_DURATION: Duration = 3.minutes

        private const val MAX_EXPONENT = 30
        private const val NANOS_PER_MILLI = 1_000_000L
    }
}
