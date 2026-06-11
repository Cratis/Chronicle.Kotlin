// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.correlation

import java.util.UUID

/**
 * Represents a correlation identifier used to track operations across call boundaries.
 */
@JvmInline
value class CorrelationId(val value: UUID) {
    companion object {
        /** Creates a new unique [CorrelationId]. */
        fun create(): CorrelationId = CorrelationId(UUID.randomUUID())

        /** A well-known [CorrelationId] representing an empty/not-set value. */
        val notSet: CorrelationId = CorrelationId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
    }

    override fun toString(): String = value.toString()
}
