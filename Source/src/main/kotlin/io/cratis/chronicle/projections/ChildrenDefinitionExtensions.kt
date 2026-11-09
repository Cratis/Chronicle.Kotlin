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
        val ordering = resolveChildrenOrdering(prop.name, childClass, annotations)

        // A [RemovedWith]/[RemovedWithJoin] placed on the same property as [ChildrenFrom] removes a
        // single child from this collection, rather than the whole read model instance.
        val removedWith = buildRemovedWithPairs(prop.findAnnotations<RemovedWith>())
        val removedWithJoin = buildRemovedWithJoinPairs(prop.findAnnotations<RemovedWithJoin>())
        val autoMapEnabled = childClass.findAnnotation<NoAutoMap>() == null && readModelClass.findAnnotation<NoAutoMap>() == null

        result[prop.name] = ProjectionsOuterClass.ChildrenDefinition.newBuilder()
            .applyOrdering(prop.name, ordering)
            .setIdentifiedBy(wireKey(identifiedBy))
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
internal fun buildChildrenMapFromEntries(
    entries: List<ChildrenEntry>,
    orderings: Map<String, ChildrenOrdering> = emptyMap(),
): Map<String, ProjectionsOuterClass.ChildrenDefinition> =
    entries.associate { entry ->
        val fromPairs = entry.fromEntries.mapNotNull { fe -> buildFromPair(fe.eventClass, fe.key, fe.properties, fe.parentKey) }
        entry.propertyName to ProjectionsOuterClass.ChildrenDefinition.newBuilder()
            .applyOrdering(entry.propertyName, orderings[entry.propertyName])
            .setIdentifiedBy(wireKey(entry.identifiedBy))
            .addAllFrom(fromPairs)
            .setAutoMap(ProjectionsOuterClass.AutoMap.Enabled)
            .build()
    }

/**
 * Resolves the single ordering the [ChildrenFrom] [annotations] of [propertyName] declare, if any.
 *
 * @throws ConflictingChildrenOrdering when one annotation declares more than one ordering, or two
 *   annotations declare different ones.
 * @throws UnknownReadModelProperty when an ordering sorts by a property [childClass] does not have.
 */
internal fun resolveChildrenOrdering(
    propertyName: String,
    childClass: KClass<*>,
    annotations: List<ChildrenFrom>,
): ChildrenOrdering? {
    var resolved: ChildrenOrdering? = null
    for (annotation in annotations) {
        val declared = buildList {
            if (annotation.prepend) add(ChildrenOrdering.prepend())
            if (annotation.orderBy.isNotEmpty()) add(sortedBy(childClass, annotation.orderBy, SortDirection.ASCENDING))
            if (annotation.orderByDescending.isNotEmpty()) {
                add(sortedBy(childClass, annotation.orderByDescending, SortDirection.DESCENDING))
            }
        }
        if (declared.isEmpty()) continue
        if (declared.size > 1) throw ConflictingChildrenOrdering(propertyName)
        if (resolved != null && resolved != declared.single()) throw ConflictingChildrenOrdering(propertyName)
        resolved = declared.single()
    }
    return resolved
}

private fun sortedBy(childClass: KClass<*>, propertyName: String, direction: SortDirection): ChildrenOrdering {
    if (childClass.memberProperties.none { it.name == propertyName }) throw UnknownReadModelProperty(childClass, propertyName)
    return ChildrenOrdering.sorted(propertyName, direction)
}

/**
 * Carries [ordering] onto the children definition of [propertyName].
 *
 * The `io.cratis:chronicle-contracts` version this client builds against has no `ChildrenDefinition.ordering`
 * field yet, so an ordering fails registration clearly instead of being dropped and leaving children in
 * arrival order. Once the contracts carry `ChildrenOrderingDefinition`, this sets it.
 *
 * @throws ChildrenOrderingNotSupportedByContracts when [ordering] is set.
 */
private fun ProjectionsOuterClass.ChildrenDefinition.Builder.applyOrdering(
    propertyName: String,
    ordering: ChildrenOrdering?,
): ProjectionsOuterClass.ChildrenDefinition.Builder {
    if (ordering != null) throw ChildrenOrderingNotSupportedByContracts(propertyName)
    return this
}

/** Extracts the element [KClass] of a collection-typed property (e.g. `List<Child>` -> `Child`). */
internal fun KProperty1<*, *>.elementClass(): KClass<*>? =
    returnType.arguments.firstOrNull()?.type?.classifier as? KClass<*>
