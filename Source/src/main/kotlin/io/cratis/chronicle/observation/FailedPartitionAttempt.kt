// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.eventSequences.EventSequenceNumber
import java.time.Instant

/**
 * One time an observer tried to get past a partition and failed.
 *
 * A partition accumulates an attempt per retry, so the list on a [FailedPartition] reads as the
 * history of the problem: when it first happened, whether the error changed, and how far the observer
 * got each time.
 *
 * @property occurred When the attempt was made.
 * @property sequenceNumber The position in the event sequence the attempt failed at.
 * @property messages The error messages from the attempt. More than one when the handler that failed
 *   raised several, which is how a batch reports every event that went wrong rather than only the first.
 * @property stackTrace The stack trace of the failure, empty when the kernel did not capture one.
 */
data class FailedPartitionAttempt(
    val occurred: Instant,
    val sequenceNumber: EventSequenceNumber,
    val messages: List<String>,
    val stackTrace: String
)
