// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.patterns

/**
 * How confident the kernel is in a pattern, from `0.0` (none) to `1.0` (certain).
 *
 * @param value The confidence.
 */
data class PatternConfidence(val value: Double) {
    override fun toString(): String = value.toString()

    companion object {
        /** No confidence. */
        @JvmField val None = PatternConfidence(0.0)

        /** Complete confidence. */
        @JvmField val Certain = PatternConfidence(1.0)
    }
}
