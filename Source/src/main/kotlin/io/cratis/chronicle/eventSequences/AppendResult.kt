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
 * @property concurrencyViolation The [ConcurrencyViolation] that occurred, if the append was rejected
 *   because the supplied `ConcurrencyScope` no longer matched the event sequence.
 * @property isSuccess Whether the append succeeded (no violations, no errors, no concurrency violation).
 */
data class AppendResult(
    val sequenceNumber: EventSequenceNumber,
    val constraintViolations: List<ConstraintViolation>,
    val errors: List<AppendError>,
    val isSuccess: Boolean,
    val concurrencyViolation: ConcurrencyViolation? = null
)
