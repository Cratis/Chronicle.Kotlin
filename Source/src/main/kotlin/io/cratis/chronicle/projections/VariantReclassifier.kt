// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberProperties

/** What one variant declared about being part of a mutually exclusive group, carried from the point
 * its definition was built to the cross-wiring pass that runs once every variant in the group is known.
 *
 * @property variantClass The variant's own read model class.
 * @property identity The type anchoring the logical identity the variants are grouped under.
 * @property enteringEventTypes The wire event types that activate this variant.
 */
internal data class VariantDeclaration(
    val variantClass: KClass<*>,
    val identity: KClass<*>,
    val enteringEventTypes: List<ProjectionsOuterClass.EventType>
)

/**
 * Turns model-bound/declarative building blocks into the reclassification and cross-wiring every
 * variant group needs, so entering one variant removes the entity from every sibling and no event
 * besides the declared entering one(s) can ever create or resurrect a variant.
 */
internal object VariantReclassifier {

    /**
     * Merges every mapping a [GlobalFor] shared handler declares into [fromPairs], keyed by event
     * type. Runs before [reclassify] so a shared mapping for a non-entering event is turned into an
     * update-only join exactly like a mapping declared directly on the variant.
     *
     * @throws GlobalHandlerPropertyNotOnVariant when a shared mapping targets a member the variant does not have.
     */
    fun mergeGlobalHandlers(
        variantClass: KClass<*>,
        fromPairs: List<ProjectionsOuterClass.KeyValuePair_EventType_FromDefinition>,
        globalHandlerFromPairs: Map<KClass<*>, List<ProjectionsOuterClass.KeyValuePair_EventType_FromDefinition>>
    ): List<ProjectionsOuterClass.KeyValuePair_EventType_FromDefinition> {
        if (globalHandlerFromPairs.isEmpty()) return fromPairs

        val memberNames = variantClass.memberProperties.map { it.name }.toSet()
        val byEventType = linkedMapOf<ProjectionsOuterClass.EventType, ProjectionsOuterClass.FromDefinition>()
        fromPairs.forEach { byEventType[it.key] = it.value }

        for ((globalHandlerClass, globalPairs) in globalHandlerFromPairs) {
            for (globalPair in globalPairs) {
                for ((propertyName, _) in globalPair.value.propertiesMap) {
                    if (propertyName !in memberNames) {
                        throw GlobalHandlerPropertyNotOnVariant(globalHandlerClass, variantClass, propertyName)
                    }
                }
                val existing = byEventType[globalPair.key]
                byEventType[globalPair.key] = if (existing != null) {
                    existing.toBuilder().putAllProperties(globalPair.value.propertiesMap).build()
                } else {
                    globalPair.value
                }
            }
        }

        return byEventType.map { (eventType, from) ->
            ProjectionsOuterClass.KeyValuePair_EventType_FromDefinition.newBuilder()
                .setKey(eventType)
                .setValue(from)
                .build()
        }
    }

    /**
     * Reclassifies every [fromPairs] entry that is not one of [enteringEventTypes] into an
     * update-only join on [variantKey]. The entering event(s) keep their ordinary create-or-update
     * `From` handler; everything else becomes a self-referential [ProjectionsOuterClass.JoinDefinition]
     * that can bring an already-active instance up to date but can never create one.
     *
     * @throws VariantMustDeclareEntersOnEvent when [enteringEventTypes] is empty.
     */
    fun reclassify(
        variantClass: KClass<*>,
        fromPairs: List<ProjectionsOuterClass.KeyValuePair_EventType_FromDefinition>,
        joinPairs: List<ProjectionsOuterClass.KeyValuePair_EventType_JoinDefinition>,
        enteringEventTypes: List<ProjectionsOuterClass.EventType>,
        variantKey: String
    ): Pair<List<ProjectionsOuterClass.KeyValuePair_EventType_FromDefinition>, List<ProjectionsOuterClass.KeyValuePair_EventType_JoinDefinition>> {
        if (enteringEventTypes.isEmpty()) {
            throw VariantMustDeclareEntersOnEvent(variantClass)
        }

        val entering = enteringEventTypes.toSet()
        val remainingFrom = mutableListOf<ProjectionsOuterClass.KeyValuePair_EventType_FromDefinition>()
        val reclassifiedJoins = mutableListOf<ProjectionsOuterClass.KeyValuePair_EventType_JoinDefinition>()

        for (pair in fromPairs) {
            if (pair.key in entering) {
                remainingFrom.add(pair)
            } else {
                val join = ProjectionsOuterClass.JoinDefinition.newBuilder()
                    .setOn(variantKey)
                    .setKey(EVENT_SOURCE_ID_KEY)
                    .putAllProperties(pair.value.propertiesMap)
                    .build()
                reclassifiedJoins.add(
                    ProjectionsOuterClass.KeyValuePair_EventType_JoinDefinition.newBuilder()
                        .setKey(pair.key)
                        .setValue(join)
                        .build()
                )
            }
        }

        return Pair(remainingFrom, joinPairs + reclassifiedJoins)
    }

    /**
     * Gives every variant a [ProjectionsOuterClass.RemovedWithDefinition] for every sibling's
     * entering event(s), and none for its own - the mutual-exclusion cross-wiring that can only run
     * once every variant of a group has been built.
     *
     * A variant cannot be told what removes it while it is being built, because its siblings are not
     * known yet. This runs once per [register] call, over every variant discovered in that call.
     */
    fun crossWireGroups(
        definitions: Map<KClass<*>, ProjectionsOuterClass.ProjectionDefinition>,
        declarations: Map<KClass<*>, VariantDeclaration>
    ): Map<KClass<*>, ProjectionsOuterClass.ProjectionDefinition> {
        if (declarations.isEmpty()) return definitions

        val byIdentity = declarations.values.groupBy { it.identity }
        val result = definitions.toMutableMap()

        for ((variantClass, declaration) in declarations) {
            val siblings = byIdentity[declaration.identity].orEmpty().filter { it.variantClass != variantClass }
            if (siblings.isEmpty()) continue

            val removedWith = siblings.flatMap { sibling ->
                sibling.enteringEventTypes.map { eventType ->
                    ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithDefinition.newBuilder()
                        .setKey(eventType)
                        .setValue(
                            ProjectionsOuterClass.RemovedWithDefinition.newBuilder()
                                .setKey(EVENT_SOURCE_ID_KEY)
                                .setParentKey(EVENT_SOURCE_ID_KEY)
                                .build()
                        )
                        .build()
                }
            }

            val existing = result.getValue(variantClass)
            result[variantClass] = existing.toBuilder().addAllRemovedWith(removedWith).build()
        }

        return result
    }

    /** The wire event type(s) named by every [EntersOn] annotation on [variantClass]. */
    fun enteringEventTypesFrom(variantClass: KClass<*>): List<ProjectionsOuterClass.EventType> =
        variantClass.findAnnotations<EntersOn>().mapNotNull { entersOn ->
            val eventKClass = entersOn.eventType
            val eventAnnotation = eventKClass.findAnnotation<io.cratis.chronicle.events.EventType>() ?: return@mapNotNull null
            val eventTypeId = eventAnnotation.id.ifEmpty { eventKClass.simpleName!! }
            toWireEventType(eventTypeId, eventAnnotation.generation)
        }
}
