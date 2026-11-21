// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.patterns

/**
 * The value of a facet.
 *
 * @param value The value as the kernel knows it. An empty value leaves the facet unspecified.
 */
data class FacetValue(val value: String) {
    /** Whether the value is set. */
    val isSpecified: Boolean get() = value.isNotEmpty()

    override fun toString(): String = value

    companion object {
        /** No value - a facet with it does not constrain anything. */
        @JvmField val Unspecified = FacetValue("")
    }
}
