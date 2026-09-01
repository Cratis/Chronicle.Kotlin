// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberProperties

/** Collects [ChildrenFrom] properties into their [ProjectionsOuterClass.ChildrenDefinition], keyed by property name. */
internal fun collectChildrenMap(readModelClass: KClass<*>): Map<String, ProjectionsOuterClass.ChildrenDefinition> {
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
            .addAllNoAutoMapProperties(NoAutoMapProperties.collectFrom(childClass))
            .setAutoMap(if (autoMapEnabled) ProjectionsOuterClass.AutoMap.Enabled else ProjectionsOuterClass.AutoMap.Disabled)
            .build()
    }
    return result
}

/** Converts [ChildrenEntry] instances collected by [ProjectionBuilderFor.children] into their wire shape. */
internal fun buildChildrenMapFromEntries(entries: List<ChildrenEntry>): Map<String, ProjectionsOuterClass.ChildrenDefinition> =
    entries.associate { entry ->
        val fromPairs = entry.fromEntries.mapNotNull { fe -> buildFromPair(fe.eventClass, fe.key, fe.properties, fe.parentKey) }
        entry.propertyName to ProjectionsOuterClass.ChildrenDefinition.newBuilder()
            .setIdentifiedBy(entry.identifiedBy)
            .addAllFrom(fromPairs)
            .setAutoMap(ProjectionsOuterClass.AutoMap.Enabled)
            .build()
    }

/** Extracts the element [KClass] of a collection-typed property (e.g. `List<Child>` -> `Child`). */
internal fun KProperty1<*, *>.elementClass(): KClass<*>? =
    returnType.arguments.firstOrNull()?.type?.classifier as? KClass<*>
