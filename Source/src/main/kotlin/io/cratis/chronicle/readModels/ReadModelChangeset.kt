// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import java.time.Instant
import java.util.UUID

/**
 * Represents a change to a read model instance, as observed through [IReadModelsService.watch].
 *
 * @param T The type of read model.
 * @property namespace The namespace the change occurred in.
 * @property modelKey The key of the read model instance that changed.
 * @property readModel The read model instance after the change, deserialized into [T], or `null`
 *   when [removed] is `true`.
 * @property removed Whether the read model instance was removed.
 * @property changeType The kind of change that occurred.
 * @property eventSequenceNumber The sequence number of the event that caused the change.
 * @property occurred When the event that caused the change occurred, if known.
 * @property correlationId The correlation identifier of the event that caused the change, if known.
 */
data class ReadModelChangeset<T>(
    val namespace: String,
    val modelKey: String,
    val readModel: T?,
    val removed: Boolean,
    val changeType: ReadModelChangeType,
    val eventSequenceNumber: Long,
    val occurred: Instant?,
    val correlationId: UUID?
)
