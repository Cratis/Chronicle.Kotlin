// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import io.cratis.chronicle.keys.Key
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation

internal class GraphEdgeBuilder<TEdge : Any>(
    private val edgeClass: KClass<TEdge>,
    private val sourceClass: KClass<*>,
    private val targetClass: KClass<*>
) : IGraphEdgeBuilder<TEdge> {
    private val from = mutableListOf<GraphEdgeFromModel>()
    private val removedWith = mutableListOf<GraphEdgeRemovedWithModel>()
    private var type: String? = null
    private var identifyingProperty: String? = null

    override fun named(type: String): IGraphEdgeBuilder<TEdge> {
        this.type = type
        return this
    }

    override fun identifiedBy(property: KProperty1<TEdge, *>): IGraphEdgeBuilder<TEdge> {
        identifyingProperty = property.name
        return this
    }

    override fun identifiedBy(propertyName: String): IGraphEdgeBuilder<TEdge> {
        identifyingProperty = requireProperty(edgeClass, propertyName)
        return this
    }

    override fun <TEvent : Any> from(
        eventClass: KClass<TEvent>,
        configure: (IGraphEdgeFromBuilder<TEdge, TEvent>) -> Unit
    ): IGraphEdgeBuilder<TEdge> {
        val builder = GraphEdgeFromBuilder(edgeClass, eventClass)
        configure(builder)
        from += GraphEdgeFromModel(
            eventClass,
            builder.sourceKey,
            builder.targetKey,
            builder.identity,
            builder.properties.toMap()
        )
        return this
    }

    override fun <TEvent : Any> removedWith(
        eventClass: KClass<TEvent>,
        configure: (IGraphEdgeRemovedWithBuilder<TEvent>) -> Unit
    ): IGraphEdgeBuilder<TEdge> {
        val builder = GraphEdgeRemovedWithBuilder(eventClass)
        configure(builder)
        removedWith += GraphEdgeRemovedWithModel(eventClass, builder.sourceKey, builder.targetKey, builder.identity)
        return this
    }

    fun build(): GraphEdgeModel {
        ModelBoundGraphReader.rejectReadModel(edgeClass)
        val members = GraphMember.of(edgeClass)
        val source = members.firstOrNull { it.has<EdgeSource>() }
        val target = members.firstOrNull { it.has<EdgeTarget>() }
        if (source == null || target == null) throw EdgeMustHaveOneSourceAndOneTarget(edgeClass)

        val keys = members.filter { it.has<Key>() }
        if (keys.size > 1) throw EdgeCanHaveOnlyOneIdentifyingKey(edgeClass)

        return GraphEdgeModel(
            edgeClass,
            type ?: edgeClass.findAnnotation<Edge>()?.value ?: edgeClass.simpleName ?: edgeClass.java.simpleName,
            sourceClass,
            targetClass,
            identifyingProperty ?: keys.singleOrNull()?.name,
            listOf(source.name, target.name),
            from.toList(),
            removedWith.toList()
        )
    }
}
