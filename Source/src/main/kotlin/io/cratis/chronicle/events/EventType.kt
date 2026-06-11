// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.events

/**
 * Annotation that marks a class as an event type and carries its Chronicle metadata.
 *
 * @property id The UUID string identifier for this event type. If empty, the class simple name is used.
 * @property generation The generation number of this event type. Defaults to 1.
 * @property tombstone Whether this event type is a tombstone (deletion marker). Defaults to false.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EventType(
    val id: String = "",
    val generation: Int = 1,
    val tombstone: Boolean = false
)
