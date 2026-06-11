// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events

/**
 * Represents the type of an event, combining its identifier, generation, and tombstone flag.
 *
 * @property id The [EventTypeId] identifying this event type.
 * @property generation The [EventTypeGeneration] of this event type.
 * @property tombstone Whether this event type is a tombstone (deletion marker).
 */
data class EventTypeDescriptor(
    val id: EventTypeId,
    val generation: EventTypeGeneration = EventTypeGeneration.first,
    val tombstone: Boolean = false
) {
    companion object {
        /** Represents an unknown event type. */
        val unknown: EventTypeDescriptor = EventTypeDescriptor(EventTypeId.unknown, EventTypeGeneration.first, false)

        /**
         * Parses a string representation into an [EventTypeDescriptor].
         *
         * Expected format: `id+generation` or `id+generation+tombstone`.
         * If only `id` is provided, generation defaults to [EventTypeGeneration.first].
         *
         * @param input The string to parse.
         * @return The parsed [EventTypeDescriptor].
         */
        fun parse(input: String): EventTypeDescriptor {
            val segments = input.split("+")
            return when (segments.size) {
                1 -> EventTypeDescriptor(EventTypeId(segments[0]), EventTypeGeneration.first, false)
                2 -> EventTypeDescriptor(EventTypeId(segments[0]), EventTypeGeneration(segments[1].toInt()), false)
                else -> EventTypeDescriptor(
                    EventTypeId(segments[0]),
                    EventTypeGeneration(segments[1].toInt()),
                    segments[2].equals("true", ignoreCase = true)
                )
            }
        }
    }

    override fun toString(): String = "${id.value}+${generation.value}"
}
