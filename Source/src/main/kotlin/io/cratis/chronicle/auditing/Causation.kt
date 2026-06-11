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
    }
}
