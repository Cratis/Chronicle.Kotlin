// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import Cratis.Chronicle.Contracts.Projections.ProjectionsGrpcKt
import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.json.chronicleGson
import io.cratis.chronicle.readModels.ReadModelsService
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

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
     * Property mappings come from [SetFrom] annotations on individual properties; structural
     * shape comes from [Join], [ChildrenFrom] and [Nested]/[ClearWith].
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
            autoMapEnabled = readModelClass.findAnnotation<NoAutoMap>() == null,
            noAutoMapProperties = readModelClass.memberProperties
                .filter { it.findAnnotation<NoAutoMap>() != null }
                .map { it.name },
            removedWith = buildRemovedWithPairs(readModelClass.findAnnotations<RemovedWith>()),
            removedWithJoin = buildRemovedWithJoinPairs(readModelClass.findAnnotations<RemovedWithJoin>()),
            all = collectFromEveryDefinition(readModelClass)
        )
    }

    /**
     * Collects [SetFrom] and arithmetic ([Count], [Increment], [Decrement], [AddFrom], [SubtractFrom])
     * property mappings that apply to a given event type.
     */
    private fun buildPropertyMappingsForEvent(readModelClass: KClass<*>, eventKClass: KClass<*>): PropertyMappings {
        val mappings = mutableMapOf<String, String>()
        var constantKey: String? = null
        for (prop in readModelClass.memberProperties) {
            for (setFrom in prop.findAnnotations<SetFrom>()) {
                val appliesToEvent = setFrom.eventType == Nothing::class || setFrom.eventType == eventKClass
                if (appliesToEvent) {
                    mappings[prop.name] = setFrom.propertyPath.ifEmpty { prop.name }
                    break // first matching annotation wins for this property
                }
            }
            prop.findAnnotations<Count>().firstOrNull { it.eventType == eventKClass }?.let { count ->
                mappings[prop.name] = "\$count"
                if (count.constantKey.isNotEmpty()) constantKey = count.constantKey
            }
            prop.findAnnotations<Increment>().firstOrNull { it.eventType == eventKClass }?.let { increment ->
                mappings[prop.name] = "\$increment"
                if (increment.constantKey.isNotEmpty()) constantKey = increment.constantKey
            }
            prop.findAnnotations<Decrement>().firstOrNull { it.eventType == eventKClass }?.let { decrement ->
                mappings[prop.name] = "\$decrement"
                if (decrement.constantKey.isNotEmpty()) constantKey = decrement.constantKey
            }
            prop.findAnnotations<AddFrom>().firstOrNull { it.eventType == eventKClass }?.let { addFrom ->
                mappings[prop.name] = "\$add(${addFrom.eventPropertyName.ifEmpty { prop.name }})"
            }
            prop.findAnnotations<SubtractFrom>().firstOrNull { it.eventType == eventKClass }?.let { subtractFrom ->
                mappings[prop.name] = "\$subtract(${subtractFrom.eventPropertyName.ifEmpty { prop.name }})"
            }
        }
        return PropertyMappings(mappings, constantKey)
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
                val mapped = buildPropertyMappingsForEvent(childClass, ann.eventType)
                buildFromPair(ann.eventType, mapped.resolvedKey(ann.key), mapped.properties, ann.parentKey)
            }
            val identifiedBy = annotations.firstOrNull { it.identifiedBy.isNotEmpty() }?.identifiedBy ?: EVENT_SOURCE_ID_KEY

            // A [RemovedWith]/[RemovedWithJoin] placed on the same property as [ChildrenFrom] removes a
            // single child from this collection, rather than the whole read model instance.
            val removedWith = buildRemovedWithPairs(prop.findAnnotations<RemovedWith>())
            val removedWithJoin = buildRemovedWithJoinPairs(prop.findAnnotations<RemovedWithJoin>())
            val autoMapEnabled = childClass.findAnnotation<NoAutoMap>() == null && readModelClass.findAnnotation<NoAutoMap>() == null

            result[prop.name] = ProjectionsOuterClass.ChildrenDefinition.newBuilder()
                .setIdentifiedBy(identifiedBy)
                .addAllFrom(fromPairs)
                .addAllRemovedWith(removedWith)
                .addAllRemovedWithJoin(removedWithJoin)
                .setAutoMap(if (autoMapEnabled) ProjectionsOuterClass.AutoMap.Enabled else ProjectionsOuterClass.AutoMap.Disabled)
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
                val mapped = buildPropertyMappingsForEvent(nestedClass, fromAnn.eventType)
                buildFromPair(fromAnn.eventType, mapped.resolvedKey(fromAnn.key), mapped.properties)
            }
            val removedWith = nestedClass.findAnnotations<ClearWith>().mapNotNull { clearWith ->
                val eventAnnotation = clearWith.eventType.findAnnotation<EventType>() ?: return@mapNotNull null
                val eventTypeId = eventAnnotation.id.ifEmpty { clearWith.eventType.simpleName!! }
                ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithDefinition.newBuilder()
                    .setKey(toWireEventType(eventTypeId, eventAnnotation.generation))
                    .setValue(ProjectionsOuterClass.RemovedWithDefinition.newBuilder().setKey(EVENT_SOURCE_ID_KEY).build())
                    .build()
            }
            val autoMapEnabled = nestedClass.findAnnotation<NoAutoMap>() == null && readModelClass.findAnnotation<NoAutoMap>() == null

            result[prop.name] = ProjectionsOuterClass.ChildrenDefinition.newBuilder()
                .addAllFrom(fromPairs)
                .addAllRemovedWith(removedWith)
                .setAutoMap(if (autoMapEnabled) ProjectionsOuterClass.AutoMap.Enabled else ProjectionsOuterClass.AutoMap.Disabled)
                .build()
        }
        return result
    }

    /** Builds [ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithDefinition] entries from [RemovedWith] annotations. */
    private fun buildRemovedWithPairs(
        annotations: List<RemovedWith>
    ): List<ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithDefinition> = annotations.mapNotNull { ann ->
        val eventAnnotation = ann.eventType.findAnnotation<EventType>() ?: return@mapNotNull null
        val eventTypeId = eventAnnotation.id.ifEmpty { ann.eventType.simpleName!! }
        ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithDefinition.newBuilder()
            .setKey(toWireEventType(eventTypeId, eventAnnotation.generation))
            .setValue(ProjectionsOuterClass.RemovedWithDefinition.newBuilder().setKey(ann.key).setParentKey(ann.parentKey).build())
            .build()
    }

    /** Builds [ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithJoinDefinition] entries from [RemovedWithJoin] annotations. */
    private fun buildRemovedWithJoinPairs(
        annotations: List<RemovedWithJoin>
    ): List<ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithJoinDefinition> = annotations.mapNotNull { ann ->
        val eventAnnotation = ann.eventType.findAnnotation<EventType>() ?: return@mapNotNull null
        val eventTypeId = eventAnnotation.id.ifEmpty { ann.eventType.simpleName!! }
        ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithJoinDefinition.newBuilder()
            .setKey(toWireEventType(eventTypeId, eventAnnotation.generation))
            .setValue(ProjectionsOuterClass.RemovedWithJoinDefinition.newBuilder().setKey(ann.key).build())
            .build()
    }

    /**
     * Collects [FromAll]/[FromEvery] properties into a single [ProjectionsOuterClass.FromEveryDefinition]
     * covering the whole projection, or `null` when neither annotation is used.
     */
    private fun collectFromEveryDefinition(readModelClass: KClass<*>): ProjectionsOuterClass.FromEveryDefinition? {
        val properties = mutableMapOf<String, String>()
        for (prop in readModelClass.memberProperties) {
            prop.findAnnotation<FromAll>()?.let { fromAll ->
                properties[prop.name] = fromEveryValue(prop.name, fromAll.property, fromAll.contextProperty)
            }
            prop.findAnnotation<FromEvery>()?.let { fromEvery ->
                properties[prop.name] = fromEveryValue(prop.name, fromEvery.property, fromEvery.contextProperty)
            }
        }
        if (properties.isEmpty()) return null
        return ProjectionsOuterClass.FromEveryDefinition.newBuilder()
            .putAllProperties(properties)
            .setIncludeChildren(true)
            .build()
    }

    private fun fromEveryValue(propertyName: String, property: String, contextProperty: String): String = when {
        contextProperty.isNotEmpty() -> "\$eventContext($contextProperty)"
        property.isNotEmpty() -> property
        else -> propertyName
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

    /** Converts [JoinDefinitionEntry] instances collected by [ProjectionBuilderFor.join] into their wire shape. */
    private fun buildJoinPairsFromEntries(entries: List<JoinDefinitionEntry>): List<ProjectionsOuterClass.KeyValuePair_EventType_JoinDefinition> =
        entries.mapNotNull { entry ->
            val eventAnnotation = entry.eventClass.findAnnotation<EventType>() ?: return@mapNotNull null
            val eventTypeId = eventAnnotation.id.ifEmpty { entry.eventClass.simpleName!! }
            val joinDef = ProjectionsOuterClass.JoinDefinition.newBuilder()
                .setOn(entry.on)
                .setKey(EVENT_SOURCE_ID_KEY)
                .putAllProperties(entry.properties)
                .build()
            ProjectionsOuterClass.KeyValuePair_EventType_JoinDefinition.newBuilder()
                .setKey(toWireEventType(eventTypeId, eventAnnotation.generation))
                .setValue(joinDef)
                .build()
        }

    /** Converts [ChildrenEntry] instances collected by [ProjectionBuilderFor.children] into their wire shape. */
    private fun buildChildrenMapFromEntries(entries: List<ChildrenEntry>): Map<String, ProjectionsOuterClass.ChildrenDefinition> =
        entries.associate { entry ->
            val fromPairs = entry.fromEntries.mapNotNull { fe -> buildFromPair(fe.eventClass, fe.key, fe.properties, fe.parentKey) }
            entry.propertyName to ProjectionsOuterClass.ChildrenDefinition.newBuilder()
                .setIdentifiedBy(entry.identifiedBy)
                .addAllFrom(fromPairs)
                .setAutoMap(ProjectionsOuterClass.AutoMap.Enabled)
                .build()
        }

    /** Converts [NestedEntry] instances collected by [ProjectionBuilderFor.nested] into their wire shape. */
    private fun buildNestedMapFromEntries(entries: List<NestedEntry>): Map<String, ProjectionsOuterClass.ChildrenDefinition> =
        entries.associate { entry ->
            val fromPairs = entry.fromEntries.mapNotNull { fe -> buildFromPair(fe.eventClass, fe.key, fe.properties) }
            val removedWith = entry.clearWithEventClasses.mapNotNull { eventClass ->
                val eventAnnotation = eventClass.findAnnotation<EventType>() ?: return@mapNotNull null
                val eventTypeId = eventAnnotation.id.ifEmpty { eventClass.simpleName!! }
                ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithDefinition.newBuilder()
                    .setKey(toWireEventType(eventTypeId, eventAnnotation.generation))
                    .setValue(ProjectionsOuterClass.RemovedWithDefinition.newBuilder().setKey(EVENT_SOURCE_ID_KEY).build())
                    .build()
            }
            entry.propertyName to ProjectionsOuterClass.ChildrenDefinition.newBuilder()
                .addAllFrom(fromPairs)
                .addAllRemovedWith(removedWith)
                .setAutoMap(ProjectionsOuterClass.AutoMap.Enabled)
                .build()
        }

    /** Converts [RemovedWithEntry] instances collected by [ProjectionBuilderFor.removedWith] into their wire shape. */
    private fun buildRemovedWithPairsFromEntries(
        entries: List<RemovedWithEntry>
    ): List<ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithDefinition> = entries.mapNotNull { entry ->
        val eventAnnotation = entry.eventClass.findAnnotation<EventType>() ?: return@mapNotNull null
        val eventTypeId = eventAnnotation.id.ifEmpty { entry.eventClass.simpleName!! }
        ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithDefinition.newBuilder()
            .setKey(toWireEventType(eventTypeId, eventAnnotation.generation))
            .setValue(ProjectionsOuterClass.RemovedWithDefinition.newBuilder().setKey(entry.key).setParentKey(entry.parentKey).build())
            .build()
    }

    /** Converts [RemovedWithJoinEntry] instances collected by [ProjectionBuilderFor.removedWithJoin] into their wire shape. */
    private fun buildRemovedWithJoinPairsFromEntries(
        entries: List<RemovedWithJoinEntry>
    ): List<ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithJoinDefinition> = entries.mapNotNull { entry ->
        val eventAnnotation = entry.eventClass.findAnnotation<EventType>() ?: return@mapNotNull null
        val eventTypeId = eventAnnotation.id.ifEmpty { entry.eventClass.simpleName!! }
        ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithJoinDefinition.newBuilder()
            .setKey(toWireEventType(eventTypeId, eventAnnotation.generation))
            .setValue(ProjectionsOuterClass.RemovedWithJoinDefinition.newBuilder().setKey(entry.key).build())
            .build()
    }

    /** Converts the accumulated [ProjectionBuilderFor.fromEveryProperties] into a [ProjectionsOuterClass.FromEveryDefinition]. */
    private fun buildFromEveryDefinitionFromEntries(properties: Map<String, String>): ProjectionsOuterClass.FromEveryDefinition? {
        if (properties.isEmpty()) return null
        return ProjectionsOuterClass.FromEveryDefinition.newBuilder()
            .putAllProperties(properties)
            .setIncludeChildren(true)
            .build()
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
            .setIsActive(true)
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

/**
 * The property mappings collected for a single event type, plus an optional constant key that
 * overrides the projection's normal per-instance key resolution (from [Count]/[Increment]/[Decrement]).
 */
private data class PropertyMappings(
    val properties: Map<String, String>,
    val constantKey: String? = null
) {
    /** Resolves the [FromDefinition] key to use: the [constantKey] wrapped as a value expression, or [default]. */
    fun resolvedKey(default: String): String = constantKey?.let { "\$value($it)" } ?: default
}

/** Accumulates the [ProjectionsOuterClass.JoinDefinition] fields contributed by every [Join]-annotated property for one event type. */
private class JoinAccumulator {
    var on: String = ""
    val properties: MutableMap<String, String> = linkedMapOf()
}

/** Extracts the element [KClass] of a collection-typed property (e.g. `List<Child>` -> `Child`). */
private fun KProperty1<*, *>.elementClass(): KClass<*>? =
    returnType.arguments.firstOrNull()?.type?.classifier as? KClass<*>
