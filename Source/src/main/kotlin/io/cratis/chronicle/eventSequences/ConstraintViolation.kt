// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

/**
 * Represents a constraint violation that was detected during an append operation.
 *
 * @property constraintId The identifier of the violated constraint.
 * @property message Human-readable description of the violation.
 * @property details Additional key/value details about the violation.
 */
data class ConstraintViolation(
    val constraintId: String,
    val message: String,
    val details: Map<String, String> = emptyMap()
)
