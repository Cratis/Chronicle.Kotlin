// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import java.util.function.Consumer
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/** Implemented by the client; applications should not implement this builder interface. */
interface IGraphNodeBuilder<TNode : Any> {
    /**
     * Says which property holds the unique key of the node, instead of the one marked
     * [io.cratis.chronicle.keys.Key].
     *
     * @param property The key property.
     * @return This builder, for chaining.
     */
    fun identifiedBy(property: KProperty1<TNode, *>): IGraphNodeBuilder<TNode>

    /**
     * The same, by property name - for callers, such as Java, that cannot produce a [KProperty1].
     *
     * @throws io.cratis.chronicle.projections.UnknownReadModelProperty when [propertyName] is not a property of [TNode].
     */
    fun identifiedBy(propertyName: String): IGraphNodeBuilder<TNode>

    /**
     * Projects [eventClass] onto the node. The node is keyed by the event source id unless the mapping says
     * [IGraphNodeFromBuilder.usingKey], and its properties are mapped from the event property of the same
     * name unless the mapping says otherwise.
     *
     * @param eventClass The event that creates or updates the node.
     * @param configure How to key the node and map its properties, or `null` to rely on the defaults.
     * @return This builder, for chaining.
     */
    fun <TEvent : Any> from(
        eventClass: KClass<TEvent>,
        configure: ((IGraphNodeFromBuilder<TNode, TEvent>) -> Unit)? = null
    ): IGraphNodeBuilder<TNode>

    /** The same, taking a Java [Class] and a [Consumer] - see [from]. */
    fun <TEvent : Any> from(
        eventClass: Class<TEvent>,
        configure: Consumer<IGraphNodeFromBuilder<TNode, TEvent>>
    ): IGraphNodeBuilder<TNode> = from(eventClass.kotlin) { configure.accept(it) }

    /** The same relying on the defaults, since Kotlin default arguments do not reach Java. */
    fun <TEvent : Any> from(eventClass: Class<TEvent>): IGraphNodeBuilder<TNode> = from(eventClass.kotlin, null)

    /**
     * Deletes the node when [eventClass] occurs. Say [IGraphNodeRemovedWithBuilder.detach] to delete the
     * edges attached to it too; without it a node that still has edges cannot be deleted.
     *
     * @param eventClass The event that deletes the node.
     * @param configure Which event property holds the key, and whether to detach, or `null` for the event
     *   source id and no detaching.
     * @return This builder, for chaining.
     */
    fun <TEvent : Any> removedWith(
        eventClass: KClass<TEvent>,
        configure: ((IGraphNodeRemovedWithBuilder<TEvent>) -> Unit)? = null
    ): IGraphNodeBuilder<TNode>

    /** The same, taking a Java [Class] and a [Consumer] - see [removedWith]. */
    fun <TEvent : Any> removedWith(
        eventClass: Class<TEvent>,
        configure: Consumer<IGraphNodeRemovedWithBuilder<TEvent>>
    ): IGraphNodeBuilder<TNode> = removedWith(eventClass.kotlin) { configure.accept(it) }

    /** The same with no configuration, since Kotlin default arguments do not reach Java. */
    fun <TEvent : Any> removedWith(eventClass: Class<TEvent>): IGraphNodeBuilder<TNode> =
        removedWith(eventClass.kotlin, null)
}
