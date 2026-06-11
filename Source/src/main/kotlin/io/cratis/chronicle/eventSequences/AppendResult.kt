// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

/**
 * Represents the result of an append operation to an event sequence.
 *
 * @property sequenceNumber The [EventSequenceNumber] assigned to the appended event.
 * @property constraintViolations Any [ConstraintViolation]s that were detected.
 * @property errors Any [AppendError]s that occurred.
 * @property isSuccess Whether the append succeeded (no violations and no errors).
 */
data class AppendResult(
    val sequenceNumber: EventSequenceNumber,
    val constraintViolations: List<ConstraintViolation>,
    val errors: List<AppendError>,
    val isSuccess: Boolean
)
