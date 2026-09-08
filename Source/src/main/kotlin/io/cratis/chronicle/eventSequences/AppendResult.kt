// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyViolation

/**
 * Represents the result of an append operation to an event sequence.
 *
 * @property sequenceNumber The [EventSequenceNumber] assigned to the appended event.
 * @property constraintViolations Any [ConstraintViolation]s that were detected.
 * @property errors Any [AppendError]s that occurred.
 * @property concurrencyViolations Every [ConcurrencyViolation] reported for the append or atomic batch.
 * @property concurrencyCheckPerformed Whether the kernel performed optimistic concurrency validation.
 * @property isSuccess Whether the append succeeded (no violations or errors).
 */
data class AppendResult(
    val sequenceNumber: EventSequenceNumber,
    val constraintViolations: List<ConstraintViolation>,
    val errors: List<AppendError>,
    val isSuccess: Boolean,
    val concurrencyViolations: List<ConcurrencyViolation> = emptyList(),
    val concurrencyCheckPerformed: Boolean = false
) {
    /** The first concurrency violation, for callers handling a single-source append. */
    val concurrencyViolation: ConcurrencyViolation? get() = concurrencyViolations.firstOrNull()

    /**
     * The position as a plain `Long`.
     *
     * [sequenceNumber] is an [EventSequenceNumber], a `@JvmInline value class`, so its getter has a
     * mangled JVM signature that Java cannot name. This one has no value class in its signature, so
     * it is how Java reads the position: `result.getSequenceNumberValue()`.
     */
    val sequenceNumberValue: Long get() = sequenceNumber.value
}
