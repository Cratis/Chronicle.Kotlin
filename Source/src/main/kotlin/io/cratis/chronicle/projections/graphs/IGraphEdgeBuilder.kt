// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import java.util.function.Consumer
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/** Implemented by the client; applications should not implement this builder interface. */
interface IGraphEdgeBuilder<TEdge : Any> {
    /**
     * Names the type of the edge, such as `MEMBER_OF`, instead of the [Edge] annotation or the class name.
     *
     * @param type The edge type.
     * @return This builder, for chaining.
     */
    fun named(type: String): IGraphEdgeBuilder<TEdge>

    /**
     * Says which property identifies the edge among edges of the same type between the same two nodes,
     * instead of the one marked [io.cratis.chronicle.keys.Key]. Every mapping of the edge then has to say
     * which event property holds the identity, or the event needs a property named like it.
     *
     * @param property The identifying property.
     * @return This builder, for chaining.
     */
    fun identifiedBy(property: KProperty1<TEdge, *>): IGraphEdgeBuilder<TEdge>

    /**
     * The same, by property name - for callers, such as Java, that cannot produce a [KProperty1].
     *
     * @throws io.cratis.chronicle.projections.UnknownReadModelProperty when [propertyName] is not a property of [TEdge].
     */
    fun identifiedBy(propertyName: String): IGraphEdgeBuilder<TEdge>

    /**
     * Projects [eventClass] onto the edge: creates it, or updates its properties when it exists. An edge
     * operation creates a bare end node when one does not exist yet.
     *
     * @param eventClass The event that creates or updates the edge.
     * @param configure Which event properties hold the keys of the end nodes, and how to map the properties.
     * @return This builder, for chaining.
     */
    fun <TEvent : Any> from(
        eventClass: KClass<TEvent>,
        configure: (IGraphEdgeFromBuilder<TEdge, TEvent>) -> Unit
    ): IGraphEdgeBuilder<TEdge>

    /** The same, taking a Java [Class] and a [Consumer] - see [from]. */
    fun <TEvent : Any> from(
        eventClass: Class<TEvent>,
        configure: Consumer<IGraphEdgeFromBuilder<TEdge, TEvent>>
    ): IGraphEdgeBuilder<TEdge> = from(eventClass.kotlin) { configure.accept(it) }

    /**
     * Deletes the edge when [eventClass] occurs. The nodes at both ends stay.
     *
     * @param eventClass The event that deletes the edge.
     * @param configure Which event properties hold the keys of the end nodes.
     * @return This builder, for chaining.
     */
    fun <TEvent : Any> removedWith(
        eventClass: KClass<TEvent>,
        configure: (IGraphEdgeRemovedWithBuilder<TEvent>) -> Unit
    ): IGraphEdgeBuilder<TEdge>

    /** The same, taking a Java [Class] and a [Consumer] - see [removedWith]. */
    fun <TEvent : Any> removedWith(
        eventClass: Class<TEvent>,
        configure: Consumer<IGraphEdgeRemovedWithBuilder<TEvent>>
    ): IGraphEdgeBuilder<TEdge> = removedWith(eventClass.kotlin) { configure.accept(it) }
}
