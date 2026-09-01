// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

/**
 * Marks a property or an event type as needing to be unique.
 *
 * On a property, uniqueness is checked across every event source - no two events of that property's
 * type may carry the same value for it. Applying [Unique] with the same [id] to a property on more
 * than one event type groups them under one constraint - the value has to be unique across all of
 * them combined, not just within each event type separately (see the class documentation for
 * [IConstraint] and the `unique` fluent builder for the equivalent hand-written form).
 *
 * On an event type, uniqueness means at most one instance of that event type may exist per event
 * source - the model-bound equivalent of a fluent constraint's `uniqueFor`.
 *
 * ```kotlin
 * @EventType
 * data class UserRegistered(@Unique(id = "UniqueEmail") val email: String, val displayName: String)
 *
 * @EventType
 * @Unique
 * data class ProjectInitialized(val name: String)
 * ```
 *
 * This mirrors .NET's `Cratis.Chronicle.Events.Constraints.UniqueAttribute`. Pair it with
 * [RemoveConstraint] on a removal event to release the value for reuse.
 *
 * @property id Name of the constraint. Defaults to the annotated property's name, or the event
 *   type's simple name when applied to a class - matching that default lets [RemoveConstraint] refer
 *   to it without the property/class having to repeat it explicitly.
 * @property message Message to use when the constraint is violated. Defaults to none.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Unique(val id: String = "", val message: String = "")
