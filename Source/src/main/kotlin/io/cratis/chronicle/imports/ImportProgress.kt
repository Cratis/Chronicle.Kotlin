// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.imports

/**
 * How far an import has come, as the kernel reports it.
 *
 * @property importId The import.
 * @property state Where the import is in its lifecycle.
 * @property eventsReceived The events the kernel has received.
 * @property eventsStaged The events staged and valid so far.
 * @property eventsCommitted The events committed to the event sequence.
 * @property alreadyAppended The events not committed because their idempotency key was already appended.
 * @property eventsRejected The events rejected.
 */
data class ImportProgress(
    val importId: ImportId,
    val state: ImportState,
    val eventsReceived: Long,
    val eventsStaged: Long,
    val eventsCommitted: Long,
    val alreadyAppended: Long,
    val eventsRejected: Long,
)
