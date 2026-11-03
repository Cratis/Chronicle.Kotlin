// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import java.time.Duration

/**
 * A partition a batch method reported it could not handle.
 *
 * @property partition The partition.
 * @property at The sequence number of the first event not handled.
 * @property kind Whether the partition failed or was deferred.
 * @property error What failed it, for a failure.
 * @property retryAfter How long to wait before delivering the partition again, for a deferral.
 */
internal data class BatchOutcome(
    val partition: String,
    val at: Long,
    val kind: Kind,
    val error: Throwable?,
    val retryAfter: Duration,
) {
    /** What a batch method reported for a partition. */
    enum class Kind {
        /** The partition failed. */
        Failed,

        /** The partition is delivered again later, without failing. */
        Deferred,
    }

    /** The outcome as a [PartitionProblem]. */
    fun toProblem(): PartitionProblem = when (kind) {
        Kind.Deferred -> PartitionProblem.deferred(partition, at, retryAfter)
        Kind.Failed -> PartitionProblem.failed(partition, at, error ?: PartitionFailedInBatch(partition), "batch method")
    }
}
