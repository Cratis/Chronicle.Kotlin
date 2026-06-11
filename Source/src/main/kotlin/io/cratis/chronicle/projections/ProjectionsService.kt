// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import Cratis.Chronicle.Contracts.Projections.ProjectionsGrpcKt
import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import com.google.gson.Gson
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.events.EventType
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.primaryConstructor

private val gson = Gson()

class ProjectionsService(
    private val eventStoreName: String,
    private val stub: ProjectionsGrpcKt.ProjectionsCoroutineStub
) : IProjectionsService {

    override suspend fun register(vararg projections: Any) {
        val definitions = projections.mapNotNull { projection ->
            buildDefinition(projection)
        }
        if (definitions.isEmpty()) return

        val request = ProjectionsOuterClass.RegisterRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setOwnerValue(1) // CLIENT
            .addAllProjections(definitions)
            .build()

        stub.register(request)
    }

    @Suppress("UNCHECKED_CAST")
    private fun buildDefinition(projection: Any): ProjectionsOuterClass.ProjectionDefinition? {
        val projectionClass = projection::class
        val annotation = projectionClass.findAnnotation<Projection>() ?: return null
        val projectionId = annotation.id.ifEmpty { projectionClass.simpleName!! }

        val readModelClass = if (annotation.readModel != Any::class) {
            annotation.readModel
        } else {
            // Try to infer from IProjectionFor<T>
            projectionClass.supertypes
                .firstOrNull { it.classifier?.toString()?.contains("IProjectionFor") == true }
                ?.arguments?.firstOrNull()?.type?.classifier as? KClass<*>
        } ?: return null

        val readModelName = readModelClass.simpleName ?: ""

        // Build initial model state JSON — create a default instance using the primary constructor
        // with all-default parameters, or fall back to an empty object.
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

        // Invoke define() to collect from entries
        val builderFor = ProjectionBuilderFor(readModelClass)
        if (projection is IProjectionFor<*>) {
            (projection as IProjectionFor<Any>).define(builderFor as ProjectionBuilderFor<Any>)
        }

        val fromPairs = builderFor.fromEntries.mapNotNull { entry ->
            val eventAnn = entry.eventClass.findAnnotation<EventType>() ?: return@mapNotNull null
            val eventTypeId = eventAnn.id.ifEmpty { entry.eventClass.simpleName!! }

            val fromDef = ProjectionsOuterClass.FromDefinition.newBuilder()
                .setKey("EventSourceId")
                .putAllProperties(entry.properties)
                .build()

            val eventType = ProjectionsOuterClass.EventType.newBuilder()
                .setId(eventTypeId)
                .setGeneration(eventAnn.generation)
                .build()

            ProjectionsOuterClass.KeyValuePair_EventType_FromDefinition.newBuilder()
                .setKey(eventType)
                .setValue(fromDef)
                .build()
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
