// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

internal class GraphNodeBuilder<TNode : Any>(private val nodeClass: KClass<TNode>) : IGraphNodeBuilder<TNode> {
    private val from = mutableListOf<GraphNodeFromModel>()
    private val removedWith = mutableListOf<GraphNodeRemovedWithModel>()
    private var keyProperty: String? = null

    override fun identifiedBy(property: KProperty1<TNode, *>): IGraphNodeBuilder<TNode> {
        keyProperty = property.name
        return this
    }

    override fun identifiedBy(propertyName: String): IGraphNodeBuilder<TNode> {
        keyProperty = requireProperty(nodeClass, propertyName)
        return this
    }

    override fun <TEvent : Any> from(
        eventClass: KClass<TEvent>,
        configure: ((IGraphNodeFromBuilder<TNode, TEvent>) -> Unit)?
    ): IGraphNodeBuilder<TNode> {
        val builder = GraphNodeFromBuilder(nodeClass, eventClass)
        configure?.invoke(builder)
        from += GraphNodeFromModel(eventClass, builder.key, builder.properties.toMap(), builder.updateOnly)
        return this
    }

    override fun <TEvent : Any> removedWith(
        eventClass: KClass<TEvent>,
        configure: ((IGraphNodeRemovedWithBuilder<TEvent>) -> Unit)?
    ): IGraphNodeBuilder<TNode> {
        val builder = GraphNodeRemovedWithBuilder(eventClass)
        configure?.invoke(builder)
        removedWith += GraphNodeRemovedWithModel(eventClass, builder.key, builder.detachEdges)
        return this
    }

    fun build(): GraphNodeModel {
        ModelBoundGraphReader.rejectReadModel(nodeClass)
        return GraphNodeModel(
            nodeClass,
            nodeClass.simpleName ?: nodeClass.java.simpleName,
            keyProperty ?: nodeKeyOf(nodeClass),
            from.toList(),
            removedWith.toList()
        )
    }
}
