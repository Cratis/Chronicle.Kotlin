// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Maps a read model property from a specific source property path on an event.
 *
 * Can be placed on a read model property to override the default auto-mapping by name.
 *
 * @property propertyPath Dot-separated path to the source property on the event.
 *   Defaults to the annotated property's own name.
 * @property eventType The specific event class this mapping applies to.
 *   Defaults to [Nothing] which means the mapping applies to all events in the read model's
 *   [FromEvent] list that contain a matching source property.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class SetFrom(
    val propertyPath: String = "",
    val eventType: KClass<*> = Nothing::class
)
