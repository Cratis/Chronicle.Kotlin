// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import io.cratis.chronicle.projections.graphs.ModelBoundGraphReader.graphNameOf
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation

/**
 * Turns what an application declares about graphs - annotated node and edge classes, and fluent
 * [IGraphProjection]s - into one [GraphRegistration] per graph.
 *
 * @param sinkTypeId The type of the graph sink that stores the graphs.
 */
internal class GraphProjections(private val sinkTypeId: String) {
    /**
     * Builds the registrations for [declarations], leaving out everything that is not a graph.
     *
     * Annotated classes that share a graph name make up one graph. Every [IGraphProjection] makes up a graph
     * of its own, identified by the projection class.
     */
    fun registrationsFor(declarations: List<Any>): List<GraphRegistration> {
        val annotated = declarations.filterIsInstance<KClass<*>>().filter { it.isGraphElement() }
        val registrations = annotated
            .groupBy { graphNameOf(it) }
            .map { (graph, types) ->
                val model = GraphModel(
                    graph,
                    types.filter { it.hasAnnotation<Node>() }.map { ModelBoundGraphReader.readNode(it) },
                    types.filter { it.hasAnnotation<Edge>() }.map { ModelBoundGraphReader.readEdge(it) }
                )
                GraphDefinitionBuilder.build(model, graph, sinkTypeId)
            }

        val fluent = declarations.filterIsInstance<IGraphProjection>().map { projection ->
            val model = GraphProjectionBuilder().build(projection)
            GraphDefinitionBuilder.build(model, projection::class.qualifiedName ?: projection::class.java.name, sinkTypeId)
        }

        return registrations + fluent
    }
}

/** Whether this class is a node or an edge of a graph. */
internal fun KClass<*>.isGraphElement(): Boolean = hasAnnotation<Node>() || hasAnnotation<Edge>()
