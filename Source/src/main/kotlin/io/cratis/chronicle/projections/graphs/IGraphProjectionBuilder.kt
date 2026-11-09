// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import java.util.function.Consumer
import kotlin.reflect.KClass

/** Implemented by the client; applications should not implement this builder interface. */
interface IGraphProjectionBuilder {
    /**
     * Declares a node type of the graph.
     *
     * @param nodeClass The node class. It needs exactly one [io.cratis.chronicle.keys.Key] property unless
     *   the node says [IGraphNodeBuilder.identifiedBy].
     * @param configure Declares the key and the events that project onto the node.
     * @return This builder, for chaining.
     */
    fun <TNode : Any> node(
        nodeClass: KClass<TNode>,
        configure: (IGraphNodeBuilder<TNode>) -> Unit
    ): IGraphProjectionBuilder

    /** The same, taking a Java [Class] and a [Consumer] - see [node]. */
    fun <TNode : Any> node(
        nodeClass: Class<TNode>,
        configure: Consumer<IGraphNodeBuilder<TNode>>
    ): IGraphProjectionBuilder = node(nodeClass.kotlin) { configure.accept(it) }

    /**
     * Declares an edge type of the graph.
     *
     * @param edgeClass The edge class. It needs a property marked [EdgeSource] and one marked [EdgeTarget].
     * @param sourceClass The node class the edge starts from.
     * @param targetClass The node class the edge ends at.
     * @param configure Declares the events that project onto the edge.
     * @return This builder, for chaining.
     */
    fun <TEdge : Any> edge(
        edgeClass: KClass<TEdge>,
        sourceClass: KClass<*>,
        targetClass: KClass<*>,
        configure: (IGraphEdgeBuilder<TEdge>) -> Unit
    ): IGraphProjectionBuilder

    /** The same, taking Java [Class]es and a [Consumer] - see [edge]. */
    fun <TEdge : Any> edge(
        edgeClass: Class<TEdge>,
        sourceClass: Class<*>,
        targetClass: Class<*>,
        configure: Consumer<IGraphEdgeBuilder<TEdge>>
    ): IGraphProjectionBuilder = edge(edgeClass.kotlin, sourceClass.kotlin, targetClass.kotlin) { configure.accept(it) }
}
