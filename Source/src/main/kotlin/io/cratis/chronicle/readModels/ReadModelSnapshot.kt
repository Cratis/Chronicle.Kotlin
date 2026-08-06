// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import Cratis.Chronicle.Contracts.ReadModels.Readmodels
import java.time.Instant
import java.util.UUID

/**
 * Represents a snapshot of a read model at a specific point in time, grouped by correlation id.
 *
 * @param T The type of read model.
 * @property instance The read model instance, deserialized into [T].
 * @property events The events that were applied to produce this snapshot.
 * @property occurred When the first event in this snapshot occurred, if known.
 * @property correlationId The correlation identifier the events were for, if known.
 */
data class ReadModelSnapshot<T>(
    val instance: T,
    val events: List<Readmodels.AppendedEvent>,
    val occurred: Instant?,
    val correlationId: UUID?
)
