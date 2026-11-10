// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.imports

import java.util.UUID

/**
 * Identifies an import session. Keep it to reattach to the session after a crash or a dropped
 * connection with [io.cratis.chronicle.eventSequences.IEventSequence.beginImport].
 *
 * @property value The underlying identifier.
 */
data class ImportId(val value: UUID) {
    override fun toString(): String = value.toString()

    companion object {
        /** The identifier of no import. */
        @JvmField
        val notSet: ImportId = ImportId(UUID(0L, 0L))

        /** Creates a new, unique [ImportId]. */
        @JvmStatic
        fun create(): ImportId = ImportId(UUID.randomUUID())
    }
}
