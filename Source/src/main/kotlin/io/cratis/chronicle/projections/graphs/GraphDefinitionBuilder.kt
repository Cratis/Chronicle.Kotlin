// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import Cratis.Chronicle.Contracts.ReadModels.Readmodels
import bcl.Bcl
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.EVENT_SOURCE_ID_EXPRESSION
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/** Builds the definitions the kernel is told about a graph from the [GraphModel] read off its declaration. */
internal object GraphDefinitionBuilder {
    /** The observer type of a read model that a projection fills. */
    private const val PROJECTION_OBSERVER_TYPE = 2

    /**
     * Builds the registration of [model].
     *
     * @param model The graph to register.
     * @param projectionId The identifier of the projection that fills the graph.
     * @param sinkTypeId The type of the graph sink that stores the graph.
     */
    fun build(model: GraphModel, projectionId: String, sinkTypeId: String): GraphRegistration {
        val graph = ProjectionsOuterClass.GraphProjectionDefinition.newBuilder()
        model.nodes.forEach { graph.putNodes(it.label, node(it)) }
        model.edges.forEach { graph.putEdges(it.edgeType, edge(it)) }

        val readModel = Readmodels.ReadModelDefinition.newBuilder()
            .setType(Readmodels.ReadModelType.newBuilder().setIdentifier(model.name).setGeneration(1).build())
            .setContainerName(model.name)
            .setDisplayName(model.name)
            .setSink(
                Readmodels.SinkDefinition.newBuilder()
                    // The same configuration reference every read model registers with, so the kernel
                    // sees a configuration id rather than a missing one.
                    .setConfigurationId(Bcl.Guid.newBuilder().setLo(1L).setHi(0L).build())
                    .setTypeId(sinkTypeId)
                    .build()
            )
            .setSchema(GraphSchemaWriter.write(model))
            .setObserverTypeValue(PROJECTION_OBSERVER_TYPE)
            .setObserverIdentifier(projectionId)
            .build()

        val projection = ProjectionsOuterClass.ProjectionDefinition.newBuilder()
            .setEventSequenceId(EventSequenceId.eventLog.value)
            .setIdentifier(projectionId)
            .setReadModel(model.name)
            .setIsActive(true)
            .setIsRewindable(true)
            .setInitialModelState("{}")
            .setAll(ProjectionsOuterClass.FromEveryDefinition.getDefaultInstance())
            .setGraph(graph.build())
            .build()

        return GraphRegistration(model.name, readModel, projection)
    }

    private fun node(node: GraphNodeModel): ProjectionsOuterClass.GraphNodeProjectionDefinition {
        val properties = GraphSchemaWriter.propertiesOf(node.type, emptyList())
        val result = ProjectionsOuterClass.GraphNodeProjectionDefinition.newBuilder()
        for (from in node.from) {
            result.putFrom(
                eventTypeKey(from.eventType),
                ProjectionsOuterClass.GraphNodeFromDefinition.newBuilder()
                    .setKey(from.key ?: EVENT_SOURCE_ID_EXPRESSION)
                    .putAllProperties(properties(from.eventType, properties.keys.filter { it != node.keyProperty }, from.properties))
                    .setUpdateOnly(from.updateOnly)
                    .build()
            )
        }

        for (removal in node.removedWith) {
            result.putRemovedWith(
                eventTypeKey(removal.eventType),
                ProjectionsOuterClass.GraphNodeRemovedWithDefinition.newBuilder()
                    .setKey(removal.key ?: EVENT_SOURCE_ID_EXPRESSION)
                    .setDetach(removal.detach)
                    .build()
            )
        }

        return result.build()
    }

    private fun edge(edge: GraphEdgeModel): ProjectionsOuterClass.GraphEdgeProjectionDefinition {
        val properties = GraphSchemaWriter.propertiesOf(edge.type, edge.excludedProperties)
        val result = ProjectionsOuterClass.GraphEdgeProjectionDefinition.newBuilder()
        for (from in edge.from) {
            val definition = ProjectionsOuterClass.GraphEdgeFromDefinition.newBuilder()
                .setSourceKey(from.sourceKey ?: EVENT_SOURCE_ID_EXPRESSION)
                .setTargetKey(from.targetKey ?: EVENT_SOURCE_ID_EXPRESSION)
                .putAllProperties(properties(from.eventType, properties.keys.filter { it != edge.identifyingProperty }, from.properties))
            identity(edge, from.eventType, from.identity)?.let { definition.setIdentity(it) }
            result.putFrom(eventTypeKey(from.eventType), definition.build())
        }

        for (removal in edge.removedWith) {
            val definition = ProjectionsOuterClass.GraphEdgeRemovedWithDefinition.newBuilder()
                .setSourceKey(removal.sourceKey ?: EVENT_SOURCE_ID_EXPRESSION)
                .setTargetKey(removal.targetKey ?: EVENT_SOURCE_ID_EXPRESSION)
            identity(edge, removal.eventType, removal.identity)?.let { definition.setIdentity(it) }
            result.putRemovedWith(eventTypeKey(removal.eventType), definition.build())
        }

        return result.build()
    }

    /** An edge with an identifying property needs every event to say where the identity is. */
    private fun identity(edge: GraphEdgeModel, eventType: KClass<*>, identity: String?): String? {
        val identifying = edge.identifyingProperty ?: return null
        return identity ?: eventPropertyNamed(eventType, identifying)
            ?: throw UnknownGraphEventProperty(edge.type, eventType, identifying)
    }

    /**
     * The mappings of one event: what the declaration says explicitly, and for every other property of the
     * graph element the event property of the same name.
     */
    private fun properties(
        eventType: KClass<*>,
        candidates: Collection<String>,
        explicit: Map<String, String>
    ): Map<String, String> {
        val result = linkedMapOf<String, String>()
        for (name in candidates) {
            val expression = explicit[name] ?: eventPropertyNamed(eventType, name)
            if (expression != null) result[name] = expression
        }

        explicit.forEach { (name, expression) -> result.putIfAbsent(name, expression) }
        return result
    }

    /** The event property that maps onto a graph property of the same name - unless it is an object. */
    private fun eventPropertyNamed(eventType: KClass<*>, graphProperty: String): String? {
        val property = eventType.memberProperties.firstOrNull { it.name == graphProperty } ?: return null
        return if (GraphSchemaWriter.isObjectProperty(eventType, property.name)) null else property.name
    }

    /** The key of an event type in the definition: its identifier and generation, such as `PersonRegistered+1`. */
    fun eventTypeKey(eventType: KClass<*>): String {
        val annotation = eventType.findAnnotation<EventType>()
            ?: throw IllegalArgumentException("'${eventType.displayName()}' is not an event type; annotate it with @EventType.")
        return "${annotation.id.ifEmpty { eventType.simpleName!! }}+${annotation.generation}"
    }
}
