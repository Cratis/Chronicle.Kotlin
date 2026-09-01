// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

/**
 * Marks an event type as releasing a named [Unique] constraint when it is appended - typically a
 * deletion or lifecycle-ending event, freeing the value the constraint protected for reuse.
 *
 * ```kotlin
 * @EventType
 * data class UserRegistered(@Unique(id = "UniqueEmail") val email: String)
 *
 * @EventType
 * @RemoveConstraint("UniqueEmail")
 * data class UserRemoved(val userId: String)
 * ```
 *
 * Repeatable, so one event can release more than one constraint.
 *
 * This mirrors .NET's `Cratis.Chronicle.Events.Constraints.RemoveConstraintAttribute`. Unlike .NET,
 * only one event type may release a given constraint name here - the wire message this client's
 * pinned `chronicle-contracts` version targets carries a single releasing event type per constraint,
 * not a set of them. Registration keeps the first event type it finds for a name and ignores any
 * others declaring the same one, rather than silently overwriting on every reconnect.
 *
 * @property value Name of the [Unique] constraint to release - matches [Unique.id].
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@JvmRepeatable(RemoveConstraints::class)
annotation class RemoveConstraint(val value: String)
