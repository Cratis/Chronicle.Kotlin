// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

/**
 * Represents the name of a namespace within an event store.
 */
@JvmInline
value class EventStoreNamespaceName(val value: String) {
    companion object {
        /** The default namespace name used when none is specified. */
        val default: EventStoreNamespaceName = EventStoreNamespaceName("Default")
    }

    override fun toString(): String = value
}
