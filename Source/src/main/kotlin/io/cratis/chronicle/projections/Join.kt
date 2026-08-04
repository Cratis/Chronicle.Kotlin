// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Populates a read model property by joining against another event type on its event source id.
 *
 * Use this when the triggering event doesn't carry the read model's own key but instead
 * references another entity by id — e.g. a child needing a property from the parent it references.
 *
 * @property eventType The event class to join against.
 * @property on The property on the read model to join on. Defaults to the annotated property's own name.
 * @property eventPropertyName The property on [eventType] to read the value from.
 *   Defaults to the annotated property's own name.
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class Join(
    val eventType: KClass<*>,
    val on: String = "",
    val eventPropertyName: String = ""
)
