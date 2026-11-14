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
     * of its own, identified by the projection class. Views are checked, but a view that is invalid does not
     * stop anything here; see [declarationsFor].
     */
    fun registrationsFor(declarations: List<Any>): List<GraphRegistration> = declarationsFor(declarations).registrations

    /**
     * Builds everything [declarations] declare about graphs: the registration of every graph and, for every
     * view that breaks a rule, the failure. An invalid view never stops a graph from being registered.
     */
    fun declarationsFor(declarations: List<Any>): GraphDeclarations {
        val annotated = declarations.filterIsInstance<KClass<*>>().filter { it.isGraphElement() }
        val models = annotated
            .groupBy { graphNameOf(it) }
            .map { (graph, types) ->
                GraphModel(
                    graph,
                    types.filter { it.hasAnnotation<Node>() }.map { ModelBoundGraphReader.readNode(it) },
                    types.filter { it.hasAnnotation<Edge>() }.map { ModelBoundGraphReader.readEdge(it) }
                ) to graph
            }
        val fluent = declarations.filterIsInstance<IGraphProjection>().map { projection ->
            GraphProjectionBuilder().build(projection) to (projection::class.qualifiedName ?: projection::class.java.name)
        }

        val outcome = GraphViewValidator(
            (models + fluent).map { it.first },
            declarations.filterIsInstance<KClass<*>>().filter { it.isGraphView() }
        ).validate()

        val registrations = (models + fluent).map { (model, projectionId) ->
            GraphDefinitionBuilder.build(model, projectionId, sinkTypeId)
                .copy(views = outcome.definitions[model.name] ?: GraphViewDefinition(model.name, emptyList()))
        }
        return GraphDeclarations(registrations, outcome.failures)
    }
}

/** Whether this class is a node or an edge of a graph. */
internal fun KClass<*>.isGraphElement(): Boolean = hasAnnotation<Node>() || hasAnnotation<Edge>()
