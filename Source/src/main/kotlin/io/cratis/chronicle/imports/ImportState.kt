// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.imports

/** Where an import session is in its lifecycle. */
enum class ImportState {
    /** Begun; nothing staged yet. */
    Open,

    /** Chunks are being staged. */
    Staging,

    /** Staging is closed and the whole import is being validated. */
    Validating,

    /** The staged events are being committed to the event sequence. */
    Committing,

    /** Every event that was not rejected is committed. */
    Completed,

    /** The import failed; with [OnInvalidEvent.Reject] nothing is committed. */
    Failed,

    /** The import was abandoned and its staging discarded; nothing is committed. */
    Abandoned;

    /** Whether the import has stopped moving: [Completed], [Failed] or [Abandoned]. */
    val isTerminal: Boolean get() = this == Completed || this == Failed || this == Abandoned
}
