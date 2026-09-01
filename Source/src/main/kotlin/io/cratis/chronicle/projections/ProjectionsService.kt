// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import Cratis.Chronicle.Contracts.Projections.ProjectionsGrpcKt
import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.json.chronicleGson
import io.cratis.chronicle.readModels.Passive
import io.cratis.chronicle.readModels.ReadModelsService
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.primaryConstructor

/** The literal key value meaning "correlate on the event source id", matching the kernel's key convention. */
internal const val EVENT_SOURCE_ID_KEY = "EventSourceId"

class ProjectionsService(
    private val eventStoreName: String,
    private val stub: ProjectionsGrpcKt.ProjectionsCoroutineStub,
    private val readModels: ReadModelsService,
    private val namespace: String = io.cratis.chronicle.EventStoreNamespaceName.default.value
) : IProjectionsService {

    override suspend fun query(declaration: String, eventSequenceId: EventSequenceId): ProjectionQueryResult {
        val request = ProjectionsOuterClass.PreviewProjectionRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespace)
            .setEventSequenceId(eventSequenceId.value)
            .setDeclaration(declaration)
            .build()

        val response = stub.preview(request)

        // The kernel answers with one of two things. A declaration it could not parse is an ordinary
        // outcome of asking a question in a language, not an exception - the errors are what you show
        // whoever wrote it.
        return if (response.hasValue1() && response.value1.errorsCount > 0) {
            ProjectionQueryResult.Invalid(
                response.value1.errorsList.map {
                    ProjectionDeclarationError(it.message, it.line, it.column)
                }
            )
        } else {
            ProjectionQueryResult.Projected(response.value0.readModelEntriesList.toList())
        }
    }

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
        val registration = ProjectionRegistration.from(projectionClass)
        val projectionId = registration.id

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

        return buildProjectionDefinition(
            projectionId,
            registration.eventSequenceId,
            readModelClass,
            fromPairs,
            joinPairs = buildJoinPairsFromEntries(builderFor.joinEntries),
            children = buildChildrenMapFromEntries(builderFor.childrenEntries),
            nested = buildNestedMapFromEntries(builderFor.nestedEntries),
            isRewindable = builderFor.isRewindable,
            removedWith = buildRemovedWithPairsFromEntries(builderFor.removedWithEntries),
            removedWithJoin = buildRemovedWithJoinPairsFromEntries(builderFor.removedWithJoinEntries),
            all = buildFromEveryDefinitionFromEntries(builderFor.fromEveryProperties)
        )
    }

    /**
     * Builds a projection definition from a read model class annotated with [FromEvent].
     * The projection identifier defaults to the class simple name; use [Projection] on the class
     * to override it (e.g. after a rename).
     * Property mappings come from [SetFrom]/[SetFromContext]/[SetValue] annotations on individual
     * properties; structural shape comes from [Join], [ChildrenFrom] and [Nested]/[ClearWith].
     */
    private suspend fun buildModelBoundDefinition(readModelClass: KClass<*>): ProjectionsOuterClass.ProjectionDefinition? {
        val fromEventAnnotations = readModelClass.findAnnotations<FromEvent>()
        if (fromEventAnnotations.isEmpty()) return null

        val registration = ProjectionRegistration.from(readModelClass)
        val projectionId = registration.id

        val fromPairs = fromEventAnnotations.mapNotNull { fromAnn ->
            val mapped = buildPropertyMappingsForEvent(readModelClass, fromAnn.eventType)
            buildFromPair(fromAnn.eventType, mapped.resolvedKey(fromAnn.key), mapped.properties)
        }

        readModels.registerWithObserver(readModelClass, 2, projectionId)

        return buildProjectionDefinition(
            projectionId,
            registration.eventSequenceId,
            readModelClass,
            fromPairs,
            joinPairs = collectJoinPairs(readModelClass),
            children = collectChildrenMap(readModelClass),
            nested = collectNestedMap(readModelClass),
            isRewindable = readModelClass.findAnnotation<NotRewindable>() == null,
            isActive = readModelClass.findAnnotation<Passive>() == null,
            autoMapEnabled = readModelClass.findAnnotation<NoAutoMap>() == null,
            noAutoMapProperties = NoAutoMapProperties.collectFrom(readModelClass),
            removedWith = buildRemovedWithPairs(readModelClass.findAnnotations<RemovedWith>()),
            removedWithJoin = buildRemovedWithJoinPairs(readModelClass.findAnnotations<RemovedWithJoin>()),
            all = collectFromEveryDefinition(readModelClass)
        )
    }

    private fun buildProjectionDefinition(
        projectionId: String,
        eventSequenceId: String,
        readModelClass: KClass<*>,
        fromPairs: List<ProjectionsOuterClass.KeyValuePair_EventType_FromDefinition>,
        joinPairs: List<ProjectionsOuterClass.KeyValuePair_EventType_JoinDefinition> = emptyList(),
        children: Map<String, ProjectionsOuterClass.ChildrenDefinition> = emptyMap(),
        nested: Map<String, ProjectionsOuterClass.ChildrenDefinition> = emptyMap(),
        isRewindable: Boolean = true,
        isActive: Boolean = true,
        autoMapEnabled: Boolean = true,
        noAutoMapProperties: List<String> = emptyList(),
        removedWith: List<ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithDefinition> = emptyList(),
        removedWithJoin: List<ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithJoinDefinition> = emptyList(),
        all: ProjectionsOuterClass.FromEveryDefinition? = null
    ): ProjectionsOuterClass.ProjectionDefinition {
        val readModelName = readModelClass.simpleName ?: ""
        val initialModelStateJson = try {
            val ctor = readModelClass.primaryConstructor
            if (ctor != null && ctor.parameters.all { it.isOptional }) {
                chronicleGson.toJson(ctor.callBy(emptyMap()))
            } else {
                "{}"
            }
        } catch (_: Exception) {
            "{}"
        }

        val builder = ProjectionsOuterClass.ProjectionDefinition.newBuilder()
            .setIdentifier(projectionId)
            .setReadModel(readModelName)
            .setInitialModelState(initialModelStateJson)
            .setEventSequenceId(eventSequenceId)
            .setIsActive(isActive)
            .setIsRewindable(isRewindable)
            .addAllFrom(fromPairs)
            .addAllJoin(joinPairs)
            .putAllChildren(children)
            .putAllNested(nested)
            .addAllRemovedWith(removedWith)
            .addAllRemovedWithJoin(removedWithJoin)
            .addAllNoAutoMapProperties(noAutoMapProperties)
            .setAutoMap(if (autoMapEnabled) ProjectionsOuterClass.AutoMap.Enabled else ProjectionsOuterClass.AutoMap.Disabled)
        if (all != null) builder.setAll(all)
        return builder.build()
    }
}
