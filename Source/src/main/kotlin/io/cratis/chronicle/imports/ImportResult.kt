// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.imports

/**
 * What an import came to.
 *
 * @property importId The import.
 * @property state Where the import ended up; [ImportState.Completed] when it succeeded.
 * @property eventsStaged The events staged.
 * @property eventsCommitted The events committed to the event sequence.
 * @property alreadyAppended The events not committed because their idempotency key was already appended.
 * @property firstSequenceNumber The sequence number of the first committed event, or `null` when none was committed.
 * @property lastSequenceNumber The sequence number of the last committed event, or `null` when none was committed.
 * @property rejectedCount The number of events rejected.
 * @property rejections The rejected events, by their index in the import.
 * @property failureReason Why the import failed, or `null`.
 */
data class ImportResult @JvmOverloads constructor(
    val importId: ImportId,
    val state: ImportState,
    val eventsStaged: Long = 0,
    val eventsCommitted: Long = 0,
    val alreadyAppended: Long = 0,
    val firstSequenceNumber: Long? = null,
    val lastSequenceNumber: Long? = null,
    val rejectedCount: Long = 0,
    val rejections: List<ImportRejection> = emptyList(),
    val failureReason: String? = null,
) {
    /** Whether the import completed. */
    val isSuccess: Boolean get() = state == ImportState.Completed
}
