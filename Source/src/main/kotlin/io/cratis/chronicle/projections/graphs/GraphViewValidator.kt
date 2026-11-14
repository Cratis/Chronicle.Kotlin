// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation

/** What checking the views of a set of graphs produced: a definition per graph, and a failure per invalid view. */
internal data class GraphViewOutcome(
    val definitions: Map<String, GraphViewDefinition>,
    val failures: List<InvalidGraphView>
)

/**
 * Checks views against the graphs they read and turns the valid ones into [GraphViewDefinition]s.
 *
 * Every view is checked on its own: an invalid view is reported and left out, and neither stops the other
 * views nor the graphs from being used.
 */
internal class GraphViewValidator(private val models: List<GraphModel>, viewTypes: List<KClass<*>>) {
    private val shapes = viewTypes.distinct().map { GraphViewShape.read(it) }.sortedWith(compareBy({ it.name }, { it.type.java.name }))
    private val graphOf = shapes.associate { shape ->
        shape.type to models.firstOrNull { model -> model.nodes.any { it.type == shape.root } }
    }
    private val shapeOf = shapes.associateBy { it.type }

    fun validate(): GraphViewOutcome {
        val failures = mutableListOf<InvalidGraphView>()
        val valid = linkedMapOf<String, MutableList<GraphView>>()
        for (shape in shapes) {
            val failure = failureOf(shape)
            if (failure != null) {
                failures += failure
            } else {
                valid.getOrPut(graphOf.getValue(shape.type)!!.name) { mutableListOf() } += build(shape)
            }
        }
        val definitions = valid.mapValues { (graph, views) ->
            GraphViewDefinition(
                graph,
                views.sortedWith { left, right -> left.name.compareTo(right.name) },
                schemaOf(models.first { it.name == graph })
            )
        }
        return GraphViewOutcome(definitions, failures)
    }

    private fun schemaOf(model: GraphModel): GraphViewSchema {
        val nodes = model.nodes.map { GraphViewSchemaNode(it.label, it.keyProperty, ordered(it.type, GraphSchemaWriter.propertiesOf(it.type, emptyList()).keys)) }
        val edges = model.edges.map {
            GraphViewSchemaEdge(
                it.edgeType,
                labelOf(model, it.sourceNode),
                labelOf(model, it.targetNode),
                it.identifyingProperty,
                ordered(it.type, GraphSchemaWriter.propertiesOf(it.type, it.excludedProperties).keys)
            )
        }
        return GraphViewSchema(nodes, edges)
    }

    private fun ordered(type: KClass<*>, names: Collection<String>): List<String> {
        val declared = GraphViewShape.propertyNamesInOrder(type)
        return names.sortedBy { declared.indexOf(it).let { index -> if (index < 0) Int.MAX_VALUE else index } }
    }

    private fun failureOf(shape: GraphViewShape): InvalidGraphView? {
        if (shape.type.hasAnnotation<Node>() || shape.type.hasAnnotation<Edge>()) return ViewIsAlsoSchema(shape.type)
        val model = graphOf[shape.type] ?: return ViewRootNotANode(shape.type, shape.root)
        if (shapes.count { graphOf[it.type] === model && it.name == shape.name } > 1) {
            return DuplicateViewName(shape.type, shape.name, model.name)
        }
        for (member in shape.members) {
            val failure = when (member.kind) {
                GraphViewMemberKind.Property -> propertyFailure(shape, model, member)
                GraphViewMemberKind.Relation -> relationFailure(shape, model, member)
                GraphViewMemberKind.EdgeProperty -> edgePropertyFailure(shape, model, member)
            }
            if (failure != null) return failure
        }
        return if (reaches(shape.type, shape.type, mutableSetOf())) RecursiveView(shape.type) else null
    }

    private fun propertyFailure(shape: GraphViewShape, model: GraphModel, member: GraphViewMemberShape): InvalidGraphView? =
        if (propertySource(model, shape, member) == null) UnknownNodeProperty(shape.type, member.name, shape.root) else null

    private fun propertySource(model: GraphModel, shape: GraphViewShape, member: GraphViewMemberShape): String? {
        val node = model.nodes.first { it.type == shape.root }
        if (member.isKey) return node.keyProperty
        return GraphSchemaWriter.propertiesOf(node.type, emptyList()).keys.firstOrNull { it.equals(member.name, ignoreCase = true) }
    }

    private fun relationFailure(shape: GraphViewShape, model: GraphModel, member: GraphViewMemberShape): InvalidGraphView? {
        val edge = model.edges.firstOrNull { it.type == member.edgeClass }
            ?: return UnknownEdge(shape.type, member.name, member.edgeClass ?: Nothing::class)
        val direction = directionOf(model, shape, edge, member)
        if (direction is Resolution.Failure) return direction.failure(shape, member, edge)

        val far = labelOf(model, if (direction == Resolution.Outgoing) edge.targetNode else edge.sourceNode)
        val target = member.target?.let { shapeOf[it] }
        if (target == null || graphOf[target.type] !== model) return RelationTargetNotAView(shape.type, member.name, member.target)
        return if (labelOf(model, target.root) != far) TargetLabelMismatch(shape.type, member.name, target.type, far) else null
    }

    private fun edgePropertyFailure(shape: GraphViewShape, model: GraphModel, member: GraphViewMemberShape): InvalidGraphView? {
        val edges = edgesLeadingTo(model, shape.type)
        if (edges.isEmpty()) return EdgePropertyViewNotUsedAsTarget(shape.type)
        if (edges.size > 1) return EdgePropertyViewSharedAcrossEdges(shape.type)
        return if (edgePropertySource(edges.single(), member) == null) {
            UnknownEdgeProperty(shape.type, member.name, edges.single().edgeType)
        } else {
            null
        }
    }

    private fun edgePropertySource(edge: GraphEdgeModel, member: GraphViewMemberShape): String? =
        GraphSchemaWriter.propertiesOf(edge.type, edge.excludedProperties).keys
            .firstOrNull { it.equals(member.edgeSource, ignoreCase = true) }

    /** The distinct edges that relations of the views of [model] follow to reach [view]. */
    private fun edgesLeadingTo(model: GraphModel, view: KClass<*>): List<GraphEdgeModel> =
        shapes.filter { graphOf[it.type] === model }
            .flatMap { it.members }
            .filter { it.kind == GraphViewMemberKind.Relation && it.target == view }
            .mapNotNull { relation -> model.edges.firstOrNull { it.type == relation.edgeClass } }
            .distinct()

    private fun reaches(from: KClass<*>, goal: KClass<*>, visited: MutableSet<KClass<*>>): Boolean {
        val shape = shapeOf[from] ?: return false
        for (target in shape.members.filter { it.kind == GraphViewMemberKind.Relation }.mapNotNull { it.target }) {
            if (target == goal) return true
            if (visited.add(target) && reaches(target, goal, visited)) return true
        }
        return false
    }

    private fun build(shape: GraphViewShape): GraphView {
        val model = graphOf.getValue(shape.type)!!
        val members = shape.members.map { member ->
            when (member.kind) {
                GraphViewMemberKind.Property ->
                    GraphViewMember(member.name, member.kind, source = propertySource(model, shape, member))
                GraphViewMemberKind.EdgeProperty ->
                    GraphViewMember(member.name, member.kind, source = edgePropertySource(edgesLeadingTo(model, shape.type).single(), member))
                GraphViewMemberKind.Relation -> {
                    val edge = model.edges.first { it.type == member.edgeClass }
                    val direction = directionOf(model, shape, edge, member)
                    GraphViewMember(
                        member.name,
                        member.kind,
                        edge = edge.edgeType,
                        direction = if (direction == Resolution.Outgoing) EdgeDirection.Outgoing else EdgeDirection.Incoming,
                        cardinality = if (member.many) GraphViewCardinality.Many else GraphViewCardinality.One,
                        view = shapeOf.getValue(member.target!!).name
                    )
                }
            }
        }
        return GraphView(shape.name, labelOf(model, shape.root), members)
    }

    private fun directionOf(model: GraphModel, shape: GraphViewShape, edge: GraphEdgeModel, member: GraphViewMemberShape): Resolution {
        val root = labelOf(model, shape.root)
        val source = labelOf(model, edge.sourceNode)
        val target = labelOf(model, edge.targetNode)
        val outgoing = root == source
        val incoming = root == target
        return when {
            !outgoing && !incoming -> Resolution.NotIncident
            outgoing && incoming -> when (member.direction) {
                FollowDirection.Outgoing -> Resolution.Outgoing
                FollowDirection.Incoming -> Resolution.Incoming
                FollowDirection.Derived -> Resolution.Ambiguous
            }
            member.direction == FollowDirection.Outgoing && !outgoing -> Resolution.NotIncident
            member.direction == FollowDirection.Incoming && !incoming -> Resolution.NotIncident
            else -> if (outgoing) Resolution.Outgoing else Resolution.Incoming
        }
    }

    private fun labelOf(model: GraphModel, node: KClass<*>): String =
        model.nodes.firstOrNull { it.type == node }?.label ?: node.simpleName ?: node.java.simpleName

    /** How the direction of a relation resolved. */
    private sealed interface Resolution {
        data object Outgoing : Resolution

        data object Incoming : Resolution

        sealed interface Failure : Resolution {
            fun failure(shape: GraphViewShape, member: GraphViewMemberShape, edge: GraphEdgeModel): InvalidGraphView
        }

        data object Ambiguous : Failure {
            override fun failure(shape: GraphViewShape, member: GraphViewMemberShape, edge: GraphEdgeModel): InvalidGraphView =
                AmbiguousDirection(shape.type, member.name, edge.edgeType)
        }

        data object NotIncident : Failure {
            override fun failure(shape: GraphViewShape, member: GraphViewMemberShape, edge: GraphEdgeModel): InvalidGraphView =
                EdgeNotIncidentOnRoot(shape.type, member.name, edge.edgeType)
        }
    }
}
