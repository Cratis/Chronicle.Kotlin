// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import io.cratis.chronicle.events.EventType
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberProperties

/** Collects [Nested] properties into their [ProjectionsOuterClass.ChildrenDefinition], keyed by property name. */
internal fun collectNestedMap(readModelClass: KClass<*>): Map<String, ProjectionsOuterClass.ChildrenDefinition> {
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
            .addAllNoAutoMapProperties(NoAutoMapProperties.collectFrom(nestedClass))
            .setAutoMap(if (autoMapEnabled) ProjectionsOuterClass.AutoMap.Enabled else ProjectionsOuterClass.AutoMap.Disabled)
            .build()
    }
    return result
}

/** Converts [NestedEntry] instances collected by [ProjectionBuilderFor.nested] into their wire shape. */
internal fun buildNestedMapFromEntries(entries: List<NestedEntry>): Map<String, ProjectionsOuterClass.ChildrenDefinition> =
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
