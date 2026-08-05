// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences.concurrency

import io.cratis.chronicle.eventSequences.EventSequenceNumber

/**
 * Represents a concurrency violation that occurred during an append operation.
 *
 * @property eventSourceId The event source id the violation occurred for.
 * @property expectedSequenceNumber The [EventSequenceNumber] the [ConcurrencyScope] expected.
 * @property actualSequenceNumber The actual [EventSequenceNumber] found by the kernel.
 */
data class ConcurrencyViolation(
    val eventSourceId: String,
    val expectedSequenceNumber: EventSequenceNumber,
    val actualSequenceNumber: EventSequenceNumber
)
