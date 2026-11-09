// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/**
 * Declares that an event creates or updates an [Edge].
 *
 * The properties of the edge are mapped by name from an event property of the same name, or explicitly
 * with the property annotations a read model already uses, naming this event.
 *
 * @property eventType The event class that creates or updates the edge.
 * @property sourceKey The event property holding the key of the source node. Defaults to the event source id.
 * @property targetKey The event property holding the key of the target node. Defaults to the event source id.
 * @property identity The event property holding the identity of the edge, for an edge type with a
 *   [io.cratis.chronicle.keys.Key]. Defaults to the event property named like that key.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
annotation class EdgeFrom(
    val eventType: KClass<*>,
    val sourceKey: String = "EventSourceId",
    val targetKey: String = "EventSourceId",
    val identity: String = ""
)
