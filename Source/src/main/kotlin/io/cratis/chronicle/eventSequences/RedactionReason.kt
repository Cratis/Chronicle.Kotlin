// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

/**
 * Represents the reason for redacting one or more events.
 */
@JvmInline
value class RedactionReason(val value: String) {
    companion object {
        /** The reason used when no specific reason was given. */
        val unknown: RedactionReason = RedactionReason("Unknown")
    }

    override fun toString(): String = value
}
