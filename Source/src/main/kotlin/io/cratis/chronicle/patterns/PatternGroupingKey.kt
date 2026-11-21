// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.patterns

/**
 * The scope patterns are mined and asked about within - normally the user.
 *
 * @param value The key as the kernel knows it.
 */
data class PatternGroupingKey(val value: String) {
    override fun toString(): String = value
}
