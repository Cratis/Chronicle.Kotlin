// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import io.cratis.chronicle.events.EventType
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberProperties

/** Accumulates the [ProjectionsOuterClass.JoinDefinition] fields contributed by every [Join]-annotated property for one event type. */
private class JoinAccumulator {
    var on: String = ""
    val properties: MutableMap<String, String> = linkedMapOf()
}

/**
 * Collects [Join] mappings, merging every property joined against the same event type into a
 * single [ProjectionsOuterClass.JoinDefinition] - matching the kernel's one-entry-per-event-type shape.
 */
internal fun collectJoinPairs(readModelClass: KClass<*>): List<ProjectionsOuterClass.KeyValuePair_EventType_JoinDefinition> {
    val accumulators = linkedMapOf<KClass<*>, JoinAccumulator>()
    for (prop in readModelClass.memberProperties) {
        for (join in prop.findAnnotations<Join>()) {
            val accumulator = accumulators.getOrPut(join.eventType) { JoinAccumulator() }
            if (accumulator.on.isEmpty()) accumulator.on = join.on.ifEmpty { prop.name }
            val resolvedEventProperty = join.eventPropertyName.ifEmpty { prop.name }
            PropertyValidator.validatePropertyExists(join.eventType, resolvedEventProperty)
            accumulator.properties[prop.name] = resolvedEventProperty
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

/** Converts [JoinDefinitionEntry] instances collected by [ProjectionBuilderFor.join] into their wire shape. */
internal fun buildJoinPairsFromEntries(entries: List<JoinDefinitionEntry>): List<ProjectionsOuterClass.KeyValuePair_EventType_JoinDefinition> =
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
