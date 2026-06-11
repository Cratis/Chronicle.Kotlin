// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events

/**
 * Represents the generation number of an event type.
 */
@JvmInline
value class EventTypeGeneration(val value: Int) {
    companion object {
        /** The first generation value. */
        const val firstValue: Int = 1

        /** The first (default) generation of any event type. */
        val first: EventTypeGeneration = EventTypeGeneration(firstValue)
    }

    override fun toString(): String = value.toString()
}
