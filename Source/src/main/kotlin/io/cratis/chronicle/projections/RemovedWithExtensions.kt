// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import io.cratis.chronicle.events.EventType
import kotlin.reflect.full.findAnnotation

/** Builds [ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithDefinition] entries from [RemovedWith] annotations. */
internal fun buildRemovedWithPairs(
    annotations: List<RemovedWith>
): List<ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithDefinition> = annotations.mapNotNull { ann ->
    val eventAnnotation = ann.eventType.findAnnotation<EventType>() ?: return@mapNotNull null
    val eventTypeId = eventAnnotation.id.ifEmpty { ann.eventType.simpleName!! }
    ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithDefinition.newBuilder()
        .setKey(toWireEventType(eventTypeId, eventAnnotation.generation))
        .setValue(
            ProjectionsOuterClass.RemovedWithDefinition.newBuilder()
                .setKey(PropertyValidator.validateKeyIfExplicit(ann.eventType, ann.key))
                .setParentKey(PropertyValidator.validateKeyIfExplicit(ann.eventType, ann.parentKey))
                .build()
        )
        .build()
}

/** Builds [ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithJoinDefinition] entries from [RemovedWithJoin] annotations. */
internal fun buildRemovedWithJoinPairs(
    annotations: List<RemovedWithJoin>
): List<ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithJoinDefinition> = annotations.mapNotNull { ann ->
    val eventAnnotation = ann.eventType.findAnnotation<EventType>() ?: return@mapNotNull null
    val eventTypeId = eventAnnotation.id.ifEmpty { ann.eventType.simpleName!! }
    ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithJoinDefinition.newBuilder()
        .setKey(toWireEventType(eventTypeId, eventAnnotation.generation))
        .setValue(
            ProjectionsOuterClass.RemovedWithJoinDefinition.newBuilder()
                .setKey(PropertyValidator.validateKeyIfExplicit(ann.eventType, ann.key))
                .build()
        )
        .build()
}

/** Converts [RemovedWithEntry] instances collected by [ProjectionBuilderFor.removedWith] into their wire shape. */
internal fun buildRemovedWithPairsFromEntries(
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
internal fun buildRemovedWithJoinPairsFromEntries(
    entries: List<RemovedWithJoinEntry>
): List<ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithJoinDefinition> = entries.mapNotNull { entry ->
    val eventAnnotation = entry.eventClass.findAnnotation<EventType>() ?: return@mapNotNull null
    val eventTypeId = eventAnnotation.id.ifEmpty { entry.eventClass.simpleName!! }
    ProjectionsOuterClass.KeyValuePair_EventType_RemovedWithJoinDefinition.newBuilder()
        .setKey(toWireEventType(eventTypeId, eventAnnotation.generation))
        .setValue(ProjectionsOuterClass.RemovedWithJoinDefinition.newBuilder().setKey(entry.key).build())
        .build()
}
