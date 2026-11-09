// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass

/** Collects the nodes and edges a fluent [IGraphProjection] declares into a [GraphModel]. */
internal class GraphProjectionBuilder : IGraphProjectionBuilder {
    private val nodes = mutableListOf<GraphNodeModel>()
    private val edges = mutableListOf<GraphEdgeModel>()

    override fun <TNode : Any> node(
        nodeClass: KClass<TNode>,
        configure: (IGraphNodeBuilder<TNode>) -> Unit
    ): IGraphProjectionBuilder {
        val builder = GraphNodeBuilder(nodeClass)
        configure(builder)
        nodes += builder.build()
        return this
    }

    override fun <TEdge : Any> edge(
        edgeClass: KClass<TEdge>,
        sourceClass: KClass<*>,
        targetClass: KClass<*>,
        configure: (IGraphEdgeBuilder<TEdge>) -> Unit
    ): IGraphProjectionBuilder {
        val builder = GraphEdgeBuilder(edgeClass, sourceClass, targetClass)
        configure(builder)
        edges += builder.build()
        return this
    }

    /** Lets [projection] declare itself, and returns the graph it declared. */
    fun build(projection: IGraphProjection): GraphModel {
        projection.define(this)
        return GraphModel(ModelBoundGraphReader.graphNameOf(projection::class), nodes.toList(), edges.toList())
    }
}
