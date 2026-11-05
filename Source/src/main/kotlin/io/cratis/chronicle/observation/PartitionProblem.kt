// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import java.time.Duration

/**
 * The first event of a partition that one method of an observer did not handle.
 *
 * @property partition The partition.
 * @property at The sequence number of the event.
 * @property state Failed or deferred.
 * @property messages The messages of a failure.
 * @property stackTrace The stack trace of a failure.
 * @property retryAfter How long to wait before delivering again, for a deferral.
 */
internal data class PartitionProblem(
    val partition: String,
    val at: Long,
    val state: PartitionState,
    val messages: List<String>,
    val stackTrace: String,
    val retryAfter: Duration,
) {
    companion object {
        /** A failure at [at], described by what [error] actually raised. */
        fun failed(partition: String, at: Long, error: Throwable, methodName: String): PartitionProblem =
            PartitionProblem(
                partition,
                at,
                PartitionState.Failed,
                listOf(error.messageFor(methodName)),
                error.unwrapReflectionFailure().stackTraceToString(),
                Duration.ZERO,
            )

        /** A deferral from [at], to be delivered again after [retryAfter]. */
        fun deferred(partition: String, at: Long, retryAfter: Duration): PartitionProblem =
            PartitionProblem(partition, at, PartitionState.Deferred, emptyList(), "", retryAfter)
    }
}
