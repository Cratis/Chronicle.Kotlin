// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Adds the value of an event property into a numeric property every time [eventType] fires.
 *
 * @property eventType The event class carrying the value to add.
 * @property eventPropertyName The property on [eventType] to read the value from.
 *   Defaults to the annotated property's own name.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class AddFrom(
    val eventType: KClass<*>,
    val eventPropertyName: String = ""
)
