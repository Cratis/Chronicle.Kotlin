// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Sets a property to a constant value when a specific event occurs, or clears it back to no value.
 *
 * Kotlin annotation parameters cannot be nullable, so this cannot mirror the .NET client's
 * `SetValueAttribute<TEvent>(object? value)` shape directly - passing a literal `null` for [value]
 * isn't expressible here. Clearing is instead expressed with [clear]: set it to `true` and leave
 * [value] unset. [value] itself carries the constant's wire representation as a plain string (the same
 * way [Count]/[Increment]/[Decrement] already carry their `constantKey` as a string), so a numeric or
 * boolean constant is written as its literal text, e.g. `"42"` or `"true"`.
 *
 * @property eventType The event class that triggers the value assignment.
 * @property value The constant value to set, as its literal string form. Ignored when [clear] is `true`.
 * @property clear When `true`, clears the property back to no value instead of setting [value]. The
 *   property should be declared nullable - a non-nullable one silently receives no value at runtime,
 *   the same way Gson leaves any other missing JSON field.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class SetValue(
    val eventType: KClass<*>,
    val value: String = "",
    val clear: Boolean = false
)
