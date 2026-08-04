// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import Cratis.Chronicle.Contracts.Projections.ProjectionsGrpcKt
import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import com.google.gson.Gson
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.readModels.ReadModelsService
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

private val gson = Gson()

/** The literal key value meaning "correlate on the event source id", matching the kernel's key convention. */
private const val EVENT_SOURCE_ID_KEY = "EventSourceId"

class ProjectionsService(
    private val eventStoreName: String,
    private val stub: ProjectionsGrpcKt.ProjectionsCoroutineStub,
    private val readModels: ReadModelsService
) : IProjectionsService {

    override suspend fun register(vararg projections: Any) {
        val definitions = projections.mapNotNull { projection ->
            @Suppress("UNCHECKED_CAST")
            when {
                projection is KClass<*> -> buildModelBoundDefinition(projection)
                projection is IProjectionFor<*> -> buildDeclarativeDefinition(projection as IProjectionFor<Any>)
                else -> null
            }
        }
        if (definitions.isEmpty()) return

        val request = ProjectionsOuterClass.RegisterRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setOwnerValue(1) // CLIENT
            .addAllProjections(definitions)
            .build()

        stub.register(request)
    }

    /**
     * Builds a projection definition from a class that implements [IProjectionFor].
     * The [Projection] annotation is optional — when absent the class simple name is used as the identifier.
     * The read model type is inferred from the [IProjectionFor] type parameter.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun buildDeclarativeDefinition(projection: IProjectionFor<Any>): ProjectionsOuterClass.ProjectionDefinition? {
        val projectionClass = projection::class
        val annotation = projectionClass.findAnnotation<Projection>()
        val projectionId = annotation?.id?.ifEmpty { projectionClass.simpleName!! } ?: projectionClass.simpleName!!

        val readModelClass = projectionClass.supertypes
            .firstOrNull { it.classifier?.toString()?.contains("IProjectionFor") == true }
            ?.arguments?.firstOrNull()?.type?.classifier as? KClass<*>
            ?: return null

        val builderFor = ProjectionBuilderFor(readModelClass as KClass<Any>)
        projection.define(builderFor)

        val fromPairs = builderFor.fromEntries.mapNotNull { entry ->
            buildFromPair(entry.eventClass, entry.key, entry.properties)
        }

        readModels.registerWithObserver(readModelClass, 2, projectionId)

        return buildProjectionDefinition(projectionId, readModelClass, fromPairs)
    }

    /**
     * Builds a projection definition from a read model class annotated with [FromEvent].
     * The projection identifier defaults to the class simple name; use [Projection] on the class
     * to override it (e.g. after a rename).
     * Property mappings come from [SetFrom] annotations on individual properties; structural
     * shape comes from [Join], [ChildrenFrom] and [Nested]/[ClearWith].
     */
    private suspend fun buildModelBoundDefinition(readModelClass: KClass<*>): ProjectionsOuterClass.ProjectionDefinition? {
        val fromEventAnnotations = readModelClass.findAnnotations<FromEvent>()
        if (fromEventAnnotations.isEmpty()) return null

        val projectionAnnotation = readModelClass.findAnnotation<Projection>()
        val projectionId = projectionAnnotation?.id?.ifEmpty { readModelClass.simpleName!! }
            ?: readModelClass.simpleName!!

        val fromPairs = fromEventAnnotations.mapNotNull { fromAnn ->
            val mappings = buildPropertyMappingsForEvent(readModelClass, fromAnn.eventType)
            buildFromPair(fromAnn.eventType, fromAnn.key, mappings)
        }

        readModels.registerWithObserver(readModelClass, 2, projectionId)

        return buildProjectionDefinition(
            projectionId,
            readModelClass,
            fromPairs,
            joinPairs = collectJoinPairs(readModelClass),
            children = collectChildrenMap(readModelClass),
            nested = collectNestedMap(readModelClass)
        )
    }

    /** Collects [SetFrom] property mappings that apply to a given event type. */
    private fun buildPropertyMappingsForEvent(readModelClass: KClass<*>, eventKClass: KClass<*>): Map<String, String> {
        val mappings = mutableMapOf<String, String>()
        for (prop in readModelClass.memberProperties) {
            for (setFrom in prop.findAnnotations<SetFrom>()) {
                val appliesToEvent = setFrom.eventType == Nothing::class || setFrom.eventType == eventKClass
                if (appliesToEvent) {
                    mappings[prop.name] = setFrom.propertyPath.ifEmpty { prop.name }
                    break // first matching annotation wins for this property
                }
            }
        }
        return mappings
    }

    /**
     * Collects [Join] mappings, merging every property joined against the same event type into a
     * single [ProjectionsOuterClass.JoinDefinition] — matching the kernel's one-entry-per-event-type shape.
     */
    private fun collectJoinPairs(readModelClass: KClass<*>): List<ProjectionsOuterClass.KeyValuePair_EventType_JoinDefinition> {
        val accumulators = linkedMapOf<KClass<*>, JoinAccumulator>()
        for (prop in readModelClass.memberProperties) {
            for (join in prop.findAnnotations<Join>()) {
                val accumulator = accumulators.getOrPut(join.eventType) { JoinAccumulator() }
                if (accumulator.on.isEmpty()) accumulator.on = join.on.ifEmpty { prop.name }
                accumulator.properties[prop.name] = join.eventPropertyName.ifEmpty { prop.name }
            }
        }
        return accumulators.mapNotNull { (eventClass, accumulator) ->
            val eventAnnotation = eventClass.findAnnotation<EventType>() ?: return@mapNotNull null
            val eventTypeId = eventAnnotation.id.ifEmpty { eventClass.simpleName!! }
            val joinDef = ProjectionsOuterClass.JoinDefinition.newBuilder()
                .setOn(accumulator.on)
                .setKey(EVENT_SOURCE_ID_KEY)
                .putAllProperties(accumulator.properties)
                .build()
            ProjectionsOuterClass.KeyValuePair_EventType_JoinDefinition.newBuilder()
                .setKey(toWireEventType(eventTypeId, eventAnnotation.generation))
                .setValue(joinDef)
                .build()
        }
    }

    /** Collects [ChildrenFrom] properties into their [ProjectionsOuterClass.ChildrenDefinition], keyed by property name. */
    private fun collectChildrenMap(readModelClass: KClass<*>): Map<String, ProjectionsOuterClass.ChildrenDefinition> {
        val result = mutableMapOf<String, ProjectionsOuterClass.ChildrenDefinition>()
        for (prop in readModelClass.memberProperties) {
            val annotations = prop.findAnnotations<ChildrenFrom>()
            if (annotations.isEmpty()) continue
            val childClass = prop.elementClass() ?: continue

            val fromPairs = annotations.mapNotNull { ann ->
                val properties = buildPropertyMappingsForEvent(childClass, ann.eventType)
                buildFromPair(ann.eventType, ann.key, properties, ann.parentKey)
            }
            val identifiedBy = annotations.firstOrNull { it.identifiedBy.isNotEmpty() }?.identifiedBy ?: EVENT_SOURCE_ID_KEY

            result[prop.name] = ProjectionsOuterClass.ChildrenDefinition.newBuilder()
                .setIdentifiedBy(identifiedBy)
                .addAllFrom(fromPairs)
                .setAutoMap(ProjectionsOuterClass.AutoMap.Enabled)
                .build()
        }
        return result
    }

    /** Collects [Nested] properties into their [ProjectionsOuterClass.ChildrenDefinition], keyed by property name. */
    private fun collectNestedMap(readModelClass: KClass<*>): Map<String, ProjectionsOuterClass.ChildrenDefinition> {
        val result = mutableMapOf<String, ProjectionsOuterClass.ChildrenDefinition>()
        for (prop in readModelClass.memberProperties) {
            if (prop.findAnnotation<Nested>() == null) continue
            val nestedClass = prop.returnType.classifier as? KClass<*> ?: continue

            val fromPairs = nestedClass.findAnnotations<FromEvent>().mapNotNull { fromAnn ->
                val mappings = buildPropertyMappingsForEvent(nestedClass, fromAnn.eventType)
                buildFromPair(fromAnn.eventType, fromAnn.key, mappings)
            }
            val removedWith = nestedClass.findAnnotations<ClearWith>().mapNotNull { clearWith ->
                val eventAnnotation = clearWith.eventType.findAnnotation<EventType>() ?: return@mapNotNull null
                val eventTypeId = eventAnnotation.id.ifEmpty { clearWith.eventType.simpleName!! }
                ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithDefinition.newBuilder()
                    .setKey(toWireEventType(eventTypeId, eventAnnotation.generation))
                    .setValue(ProjectionsOuterClass.RemovedWithDefinition.newBuilder().setKey(EVENT_SOURCE_ID_KEY).build())
                    .build()
            }

            result[prop.name] = ProjectionsOuterClass.ChildrenDefinition.newBuilder()
                .addAllFrom(fromPairs)
                .addAllRemovedWith(removedWith)
                .setAutoMap(ProjectionsOuterClass.AutoMap.Enabled)
                .build()
        }
        return result
    }

    /** Builds a single gRPC [FromDefinition] entry for the given event class and property mappings. */
    private fun buildFromPair(
        eventKClass: KClass<*>,
        key: String,
        properties: Map<String, String>,
        parentKey: String? = null
    ): ProjectionsOuterClass.KeyValuePair_EventType_FromDefinition? {
        val eventAnnotation = eventKClass.findAnnotation<EventType>() ?: return null
        val eventTypeId = eventAnnotation.id.ifEmpty { eventKClass.simpleName!! }

        val fromDefBuilder = ProjectionsOuterClass.FromDefinition.newBuilder()
            .setKey(key)
            .putAllProperties(properties)
        if (parentKey != null) fromDefBuilder.setParentKey(parentKey)

        return ProjectionsOuterClass.KeyValuePair_EventType_FromDefinition.newBuilder()
            .setKey(toWireEventType(eventTypeId, eventAnnotation.generation))
            .setValue(fromDefBuilder.build())
            .build()
    }

    private fun toWireEventType(id: String, generation: Int): ProjectionsOuterClass.EventType =
        ProjectionsOuterClass.EventType.newBuilder().setId(id).setGeneration(generation).build()

    private fun buildProjectionDefinition(
        projectionId: String,
        readModelClass: KClass<*>,
        fromPairs: List<ProjectionsOuterClass.KeyValuePair_EventType_FromDefinition>,
        joinPairs: List<ProjectionsOuterClass.KeyValuePair_EventType_JoinDefinition> = emptyList(),
        children: Map<String, ProjectionsOuterClass.ChildrenDefinition> = emptyMap(),
        nested: Map<String, ProjectionsOuterClass.ChildrenDefinition> = emptyMap()
    ): ProjectionsOuterClass.ProjectionDefinition {
        val readModelName = readModelClass.simpleName ?: ""
        val initialModelStateJson = try {
            val ctor = readModelClass.primaryConstructor
            if (ctor != null && ctor.parameters.all { it.isOptional }) {
                gson.toJson(ctor.callBy(emptyMap()))
            } else {
                "{}"
            }
        } catch (_: Exception) {
            "{}"
        }

        return ProjectionsOuterClass.ProjectionDefinition.newBuilder()
            .setIdentifier(projectionId)
            .setReadModel(readModelName)
            .setInitialModelState(initialModelStateJson)
            .setEventSequenceId(EventSequenceId.eventLog.value)
            .setIsActive(true)
            .setIsRewindable(true)
            .addAllFrom(fromPairs)
            .addAllJoin(joinPairs)
            .putAllChildren(children)
            .putAllNested(nested)
            .setAutoMap(ProjectionsOuterClass.AutoMap.Enabled)
            .build()
    }
}

/** Accumulates the [ProjectionsOuterClass.JoinDefinition] fields contributed by every [Join]-annotated property for one event type. */
private class JoinAccumulator {
    var on: String = ""
    val properties: MutableMap<String, String> = linkedMapOf()
}

/** Extracts the element [KClass] of a collection-typed property (e.g. `List<Child>` -> `Child`). */
private fun KProperty1<*, *>.elementClass(): KClass<*>? =
    returnType.arguments.firstOrNull()?.type?.classifier as? KClass<*>
