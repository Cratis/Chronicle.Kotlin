// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import java.util.UUID

/**
 * Options that can be supplied when appending events to an event sequence.
 *
 * @property correlationId Optional correlation identifier to use for this operation.
 *   If null, the current [io.cratis.chronicle.correlation.CorrelationIdManager] value is used.
 */
data class AppendOptions(
    val correlationId: UUID? = null
)
