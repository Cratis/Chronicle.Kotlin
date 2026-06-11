// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

/**
 * Represents the name of an event store.
 */
@JvmInline
value class EventStoreName(val value: String) {
    override fun toString(): String = value
}
