// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Declares that a collection property is populated with child read model instances created or
 * updated by a specific event type.
 *
 * The child element type's own [FromEvent], [SetFrom], [Count], [Increment], [Decrement],
 * [AddFrom] and [SubtractFrom] annotations describe how each child instance is built.
 *
 * @property eventType The event class that creates/updates children in this collection.
 * @property key The event property identifying which child the event applies to.
 *   Defaults to "EventSourceId".
 * @property identifiedBy The property on the child type that is the child's own identity.
 *   Defaults to the child type's `id`/`key` property, falling back to "EventSourceId".
 * @property parentKey The event property identifying the parent instance.
 *   Defaults to "EventSourceId".
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class ChildrenFrom(
    val eventType: KClass<*>,
    val key: String = "EventSourceId",
    val identifiedBy: String = "",
    val parentKey: String = "EventSourceId"
)
