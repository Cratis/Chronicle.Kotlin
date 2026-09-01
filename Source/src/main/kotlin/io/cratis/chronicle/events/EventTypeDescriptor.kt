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
         * This is also the only way a Java caller can name an event type: [EventTypeId] and
         * [EventTypeGeneration] are `@JvmInline value class`es whose constructors Java cannot
         * reach, and a `String`-taking constructor here would erase to the primary one's JVM
         * signature. `@JvmStatic` is what makes `EventTypeDescriptor.parse("Something")` compile
         * from Java without going through `Companion`.
         *
         * @param input The string to parse.
         * @return The parsed [EventTypeDescriptor].
         */
        @JvmStatic
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
