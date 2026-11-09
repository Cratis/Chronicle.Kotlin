// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import io.cratis.chronicle.keys.Key
import io.cratis.chronicle.projections.AddFrom
import io.cratis.chronicle.projections.Count
import io.cratis.chronicle.projections.Decrement
import io.cratis.chronicle.projections.EVENT_SOURCE_ID_KEY
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Increment
import io.cratis.chronicle.projections.PropertyMappings
import io.cratis.chronicle.projections.PropertyValidator
import io.cratis.chronicle.projections.RemovedWith
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.SetFromContext
import io.cratis.chronicle.projections.SetValue
import io.cratis.chronicle.projections.SubtractFrom
import io.cratis.chronicle.projections.buildPropertyMappingsForEvent
import io.cratis.chronicle.readModels.ReadModel
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties

/** Reads the annotations on node and edge classes into the model a graph definition is built from. */
internal object ModelBoundGraphReader {
    /** The graph a type belongs to: its [Graph] annotation, or otherwise its package. */
    fun graphNameOf(type: KClass<*>): String =
        type.findAnnotation<Graph>()?.value
            ?: type.java.packageName.ifEmpty { null }
            ?: type.simpleName
            ?: type.java.name

    /** Rejects a graph element that is also annotated as a read model. */
    fun rejectReadModel(type: KClass<*>) {
        if (type.hasAnnotation<ReadModel>()) throw GraphElementCannotBeReadModel(type)
    }

    fun readNode(type: KClass<*>): GraphNodeModel {
        rejectReadModel(type)
        val keyProperty = nodeKeyOf(type)
        val declared = type.findAnnotations<FromEvent>().map { annotation ->
            val mapped = buildPropertyMappingsForEvent(type, annotation.eventType)
            GraphNodeFromModel(annotation.eventType, keyOf(annotation.eventType, annotation.key, mapped), mapped.properties)
        }

        // An event that only a property refers to still projects the node, keyed by the event source id.
        val declaredEvents = declared.map { it.eventType }.toSet()
        val implicit = referencedEvents(type).filter { it !in declaredEvents }.map { event ->
            val mapped = buildPropertyMappingsForEvent(type, event)
            GraphNodeFromModel(event, keyOf(event, EVENT_SOURCE_ID_KEY, mapped), mapped.properties)
        }

        val removedWith = type.findAnnotations<RemovedWith>().map { annotation ->
            GraphNodeRemovedWithModel(
                annotation.eventType,
                PropertyValidator.validateKeyIfExplicit(annotation.eventType, annotation.key),
                annotation.detach
            )
        }

        return GraphNodeModel(type, type.simpleName ?: type.java.simpleName, keyProperty, declared + implicit, removedWith)
    }

    fun readEdge(type: KClass<*>): GraphEdgeModel {
        rejectReadModel(type)
        val members = GraphMember.of(type)
        val sources = members.filter { it.has<EdgeSource>() }
        val targets = members.filter { it.has<EdgeTarget>() }
        if (sources.size != 1 || targets.size != 1) throw EdgeMustHaveOneSourceAndOneTarget(type)

        val keys = members.filter { it.has<Key>() }
        if (keys.size > 1) throw EdgeCanHaveOnlyOneIdentifyingKey(type)

        val excluded = listOf(sources[0].name, targets[0].name)
        val from = type.findAnnotations<EdgeFrom>().map { annotation ->
            GraphEdgeFromModel(
                annotation.eventType,
                PropertyValidator.validateKeyIfExplicit(annotation.eventType, annotation.sourceKey),
                PropertyValidator.validateKeyIfExplicit(annotation.eventType, annotation.targetKey),
                identityOf(annotation.eventType, annotation.identity),
                buildPropertyMappingsForEvent(type, annotation.eventType).properties - excluded.toSet()
            )
        }
        val removedWith = type.findAnnotations<EdgeRemovedWith>().map { annotation ->
            GraphEdgeRemovedWithModel(
                annotation.eventType,
                PropertyValidator.validateKeyIfExplicit(annotation.eventType, annotation.sourceKey),
                PropertyValidator.validateKeyIfExplicit(annotation.eventType, annotation.targetKey),
                identityOf(annotation.eventType, annotation.identity)
            )
        }

        return GraphEdgeModel(
            type,
            type.findAnnotation<Edge>()!!.value,
            sources[0].find<EdgeSource>()!!.value,
            targets[0].find<EdgeTarget>()!!.value,
            keys.singleOrNull()?.name,
            excluded,
            from,
            removedWith
        )
    }

    private fun identityOf(eventType: KClass<*>, identity: String): String? =
        identity.ifEmpty { null }?.let { PropertyValidator.validatePropertyExists(eventType, it) }

    private fun keyOf(eventType: KClass<*>, key: String, mapped: PropertyMappings): String =
        mapped.resolvedKey(PropertyValidator.validateKeyIfExplicit(eventType, key))

    /** The events a property of [type] says how to map, whether or not the node declares them with [FromEvent]. */
    private fun referencedEvents(type: KClass<*>): List<KClass<*>> =
        type.memberProperties.flatMap { property ->
            property.findAnnotations<SetFrom>().map { it.eventType }.filter { it != Nothing::class } +
                property.findAnnotations<SetFromContext>().map { it.eventType } +
                property.findAnnotations<Increment>().map { it.eventType } +
                property.findAnnotations<Decrement>().map { it.eventType } +
                property.findAnnotations<Count>().map { it.eventType } +
                property.findAnnotations<AddFrom>().map { it.eventType } +
                property.findAnnotations<SubtractFrom>().map { it.eventType } +
                property.findAnnotations<SetValue>().map { it.eventType }
        }.distinct()
}
