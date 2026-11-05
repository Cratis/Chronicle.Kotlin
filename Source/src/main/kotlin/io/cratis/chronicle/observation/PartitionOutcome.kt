// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import java.time.Duration

/**
 * The outcome of observing the events of one partition of a delivery - what the client answers the
 * kernel with, one per partition.
 *
 * @property partition The partition the outcome is for.
 * @property state How the partition settled.
 * @property lastSuccessfulObservation The last event of the partition that was observed, or [NONE_OBSERVED].
 * @property messages The messages of a failure.
 * @property stackTrace The stack trace of a failure.
 * @property retryAfter How long to wait before delivering again, for a deferral.
 * @property readModelState The reduced read model as JSON, for reducers; `null` when there is none to write.
 */
internal data class PartitionOutcome(
    val partition: String,
    val state: PartitionState,
    val lastSuccessfulObservation: Long,
    val messages: List<String> = emptyList(),
    val stackTrace: String = "",
    val retryAfter: Duration = Duration.ZERO,
    val readModelState: String? = null,
) {
    companion object {
        /** An outcome where every event of [partition] failed with [error]. */
        fun failed(partition: String, error: Throwable, methodName: String): PartitionOutcome = PartitionOutcome(
            partition,
            PartitionState.Failed,
            NONE_OBSERVED,
            listOf(error.messageFor(methodName)),
            error.unwrapReflectionFailure().stackTraceToString(),
        )

        /**
         * What [lastSuccessfulObservation] holds when no event of the partition was observed: the
         * kernel's "unavailable" sequence number, the largest unsigned 64-bit value on the wire. Zero
         * would not do - it is a real sequence number, and would count the first event as handled.
         */
        const val NONE_OBSERVED: Long = -1L
    }
}
