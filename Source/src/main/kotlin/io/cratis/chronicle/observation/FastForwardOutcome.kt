// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * What happened when an observer was fast forwarded.
 */
enum class FastForwardOutcome {
    /** The observer was moved to the tail of the event sequence. */
    FastForwarded,

    /** The observer was already at the tail of the event sequence, so nothing was changed. */
    AlreadyAtTail,

    /** The observer is replaying, so it cannot be fast forwarded until the replay has completed. */
    Replaying,

    /** The observer is retired or removed, so nothing was changed. */
    NotAvailable;

    internal companion object {
        /** Maps the wire value of the outcome. Values this client does not know are [NotAvailable]. */
        fun fromWire(value: Int): FastForwardOutcome = when (value) {
            0 -> FastForwarded
            1 -> AlreadyAtTail
            2 -> Replaying
            else -> NotAvailable
        }
    }
}
