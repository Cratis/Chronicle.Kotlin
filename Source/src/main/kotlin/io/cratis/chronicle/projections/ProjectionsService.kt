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
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

private val gson = Gson()

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
     * Property mappings come from [SetFrom] annotations on individual properties.
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

        return buildProjectionDefinition(projectionId, readModelClass, fromPairs)
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

    /** Builds a single gRPC [FromDefinition] entry for the given event class and property mappings. */
    private fun buildFromPair(
        eventKClass: KClass<*>,
        key: String,
        properties: Map<String, String>
    ): ProjectionsOuterClass.KeyValuePair_EventType_FromDefinition? {
        val eventAnnotation = eventKClass.findAnnotation<EventType>() ?: return null
        val eventTypeId = eventAnnotation.id.ifEmpty { eventKClass.simpleName!! }

        val fromDef = ProjectionsOuterClass.FromDefinition.newBuilder()
            .setKey(key)
            .putAllProperties(properties)
            .build()

        val eventType = ProjectionsOuterClass.EventType.newBuilder()
            .setId(eventTypeId)
            .setGeneration(eventAnnotation.generation)
            .build()

        return ProjectionsOuterClass.KeyValuePair_EventType_FromDefinition.newBuilder()
            .setKey(eventType)
            .setValue(fromDef)
            .build()
    }

    private fun buildProjectionDefinition(
        projectionId: String,
        readModelClass: KClass<*>,
        fromPairs: List<ProjectionsOuterClass.KeyValuePair_EventType_FromDefinition>
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
            .build()
    }
}
