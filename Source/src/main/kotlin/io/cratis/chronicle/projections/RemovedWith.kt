// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Declares which event removes a read model instance, or — when placed on a [ChildrenFrom]
 * property — which event removes a single child from that collection.
 *
 * @property eventType The event class that triggers the removal.
 * @property key The event property identifying the instance/child to remove. Defaults to "EventSourceId".
 * @property parentKey The event property identifying the parent instance, when removing a child.
 *   Defaults to "EventSourceId".
 * @property detach Only for a [io.cratis.chronicle.projections.graphs.Node]: delete the edges attached to the
 *   node together with it. Without it a node that still has edges cannot be deleted. Ignored on a read model.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class RemovedWith(
    val eventType: KClass<*>,
    val key: String = "EventSourceId",
    val parentKey: String = "EventSourceId",
    val detach: Boolean = false
)
