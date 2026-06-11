// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.auditing

/**
 * Represents a type of causation, identified by its [name].
 */
@JvmInline
value class CausationType(val name: String) {
    companion object {
        /** Represents the root causation type. */
        val root: CausationType = CausationType("Root")

        /** Represents an unknown causation type. */
        val unknown: CausationType = CausationType("Unknown")

        /** Causation type used when appending a single event via the Kotlin client. */
        val appendEvent: CausationType = CausationType("KotlinClient.Append")

        /** Causation type used when appending multiple events via the Kotlin client. */
        val appendManyEvents: CausationType = CausationType("KotlinClient.AppendMany")
    }

    override fun toString(): String = name
}
