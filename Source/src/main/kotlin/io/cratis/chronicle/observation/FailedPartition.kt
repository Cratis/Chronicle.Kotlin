// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import java.util.UUID

/**
 * A partition an observer is stuck on.
 *
 * Chronicle observes each event source independently, so a handler that throws stops that one
 * partition and leaves every other event source moving. That is what keeps one bad event from
 * halting the system - and also what makes a stuck partition easy to miss, since nothing else
 * looks wrong.
 *
 * @property id The identifier the kernel holds this failure under.
 * @property observerId The observer that is failing.
 * @property partition The event source the observer cannot get past.
 * @property attempts Every attempt made so far, oldest first.
 */
data class FailedPartition(
    val id: UUID,
    val observerId: String,
    val partition: String,
    val attempts: List<FailedPartitionAttempt>
) {
    /** The most recent attempt, which is the one describing why the partition is still stuck. */
    val lastAttempt: FailedPartitionAttempt? get() = attempts.lastOrNull()
}
