// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.auditing

import java.time.Instant

/**
 * Represents a single causation entry in a causation chain.
 *
 * @property timestamp When this causation occurred.
 * @property type The [CausationType] of this causation.
 * @property properties Key/value properties associated with this causation.
 */
data class Causation(
    val timestamp: Instant,
    val type: CausationType,
    val properties: Map<String, String> = emptyMap()
) {
    companion object {
        /** Creates an unknown causation with the current timestamp. */
        fun unknown(): Causation = Causation(Instant.now(), CausationType.unknown)

        /**
         * Creates a [Causation] naming its type as a plain string.
         *
         * [CausationType] is a `@JvmInline value class`, so its constructor has a mangled JVM
         * signature and Java cannot call it - which would leave a Java caller unable to build a
         * [Causation] at all. This has no value class in its signature, so it is what Java uses.
         *
         * @param timestamp When the causation occurred.
         * @param type The name of the causation type.
         * @param properties Key/value properties associated with it.
         */
        @JvmStatic
        @JvmOverloads
        fun of(
            timestamp: Instant,
            type: String,
            properties: Map<String, String> = emptyMap()
        ): Causation = Causation(timestamp, CausationType(type), properties)
    }
}
