// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberProperties

/**
 * The property mappings collected for a single event type, plus an optional constant key that
 * overrides the projection's normal per-instance key resolution (from [Count]/[Increment]/[Decrement]).
 */
internal data class PropertyMappings(
    val properties: Map<String, String>,
    val constantKey: String? = null
) {
    /** Resolves the [ProjectionsOuterClass.FromDefinition] key to use: the [constantKey] wrapped as a value expression, or [default]. */
    fun resolvedKey(default: String): String = constantKey?.let { "\$value($it)" } ?: default
}

/**
 * Collects [SetFrom], [SetFromContext], [SetValue] and arithmetic ([Count], [Increment], [Decrement],
 * [AddFrom], [SubtractFrom]) property mappings that apply to a given event type.
 */
internal fun buildPropertyMappingsForEvent(readModelClass: KClass<*>, eventKClass: KClass<*>): PropertyMappings {
    val mappings = mutableMapOf<String, String>()
    var constantKey: String? = null
    for (prop in readModelClass.memberProperties) {
        for (setFrom in prop.findAnnotations<SetFrom>()) {
            val appliesToEvent = setFrom.eventType == Nothing::class || setFrom.eventType == eventKClass
            if (appliesToEvent) {
                val resolved = setFrom.propertyPath.ifEmpty { prop.name }
                // An explicit event type is a firm claim the property exists there, so it is worth
                // validating. The implicit Nothing::class form deliberately applies to whichever
                // subscribed events happen to carry a matching property, so it is not.
                if (setFrom.eventType == eventKClass) PropertyValidator.validatePropertyExists(eventKClass, resolved)
                mappings[prop.name] = resolved
                break
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
            val resolved = addFrom.eventPropertyName.ifEmpty { prop.name }
            PropertyValidator.validatePropertyExists(eventKClass, resolved)
            mappings[prop.name] = "\$add($resolved)"
        }
        prop.findAnnotations<SubtractFrom>().firstOrNull { it.eventType == eventKClass }?.let { subtractFrom ->
            val resolved = subtractFrom.eventPropertyName.ifEmpty { prop.name }
            PropertyValidator.validatePropertyExists(eventKClass, resolved)
            mappings[prop.name] = "\$subtract($resolved)"
        }
        prop.findAnnotations<SetFromContext>().firstOrNull { it.eventType == eventKClass }?.let { setFromContext ->
            val resolved = setFromContext.contextProperty.ifEmpty { prop.name }
            PropertyValidator.validatePropertyExists(EventContext::class, resolved)
            mappings[prop.name] = "\$eventContext($resolved)"
        }
        prop.findAnnotations<SetValue>().firstOrNull { it.eventType == eventKClass }?.let { setValue ->
            // Kotlin's reflection has no reliable way to tell a genuinely non-nullable property from an
            // unannotated Java one - a plain Java `String` reports isMarkedNullable == false exactly like
            // a Kotlin non-null `String` does, so rejecting the latter would reject the former too.
            // Declaring the property nullable is the author's responsibility, same as .NET treats a
            // member compiled outside a nullable-aware context as able to hold null.
            mappings[prop.name] = if (setValue.clear) "\$null" else "\$value(${setValue.value})"
        }
    }
    return PropertyMappings(mappings, constantKey)
}

/** Builds a single gRPC [ProjectionsOuterClass.FromDefinition] entry for the given event class and property mappings. */
internal fun buildFromPair(
    eventKClass: KClass<*>,
    key: String,
    properties: Map<String, String>,
    parentKey: String? = null
): ProjectionsOuterClass.KeyValuePair_EventType_FromDefinition? {
    val eventAnnotation = eventKClass.findAnnotation<EventType>() ?: return null
    val eventTypeId = eventAnnotation.id.ifEmpty { eventKClass.simpleName!! }

    val fromDefBuilder = ProjectionsOuterClass.FromDefinition.newBuilder()
        .setKey(PropertyValidator.validateKeyIfExplicit(eventKClass, key))
        .putAllProperties(properties)
    if (parentKey != null) fromDefBuilder.setParentKey(PropertyValidator.validateKeyIfExplicit(eventKClass, parentKey))

    return ProjectionsOuterClass.KeyValuePair_EventType_FromDefinition.newBuilder()
        .setKey(toWireEventType(eventTypeId, eventAnnotation.generation))
        .setValue(fromDefBuilder.build())
        .build()
}

internal fun toWireEventType(id: String, generation: Int): ProjectionsOuterClass.EventType =
    ProjectionsOuterClass.EventType.newBuilder().setId(id).setGeneration(generation).build()

/**
 * Collects [FromAll]/[FromEvery] properties into a single [ProjectionsOuterClass.FromEveryDefinition]
 * covering the whole projection, or `null` when neither annotation is used.
 */
internal fun collectFromEveryDefinition(readModelClass: KClass<*>): ProjectionsOuterClass.FromEveryDefinition? {
    val properties = mutableMapOf<String, String>()
    for (prop in readModelClass.memberProperties) {
        prop.findAnnotation<FromAll>()?.let { fromAll ->
            if (fromAll.contextProperty.isNotEmpty()) PropertyValidator.validatePropertyExists(EventContext::class, fromAll.contextProperty)
            properties[prop.name] = fromEveryValue(prop.name, fromAll.property, fromAll.contextProperty)
        }
        prop.findAnnotation<FromEvery>()?.let { fromEvery ->
            if (fromEvery.contextProperty.isNotEmpty()) PropertyValidator.validatePropertyExists(EventContext::class, fromEvery.contextProperty)
            properties[prop.name] = fromEveryValue(prop.name, fromEvery.property, fromEvery.contextProperty)
        }
        prop.findAnnotation<FromEventSourceId>()?.let {
            properties[prop.name] = EVENT_SOURCE_ID
        }
    }
    if (properties.isEmpty()) return null
    return ProjectionsOuterClass.FromEveryDefinition.newBuilder()
        .putAllProperties(properties)
        .setIncludeChildren(true)
        .build()
}

/** What the kernel resolves to the event source id of the event being projected. */
private const val EVENT_SOURCE_ID = "\$eventSourceId"

private fun fromEveryValue(propertyName: String, property: String, contextProperty: String): String = when {
    contextProperty.isNotEmpty() -> "\$eventContext($contextProperty)"
    property.isNotEmpty() -> property
    else -> propertyName
}

/** Converts the accumulated declarative `fromEvery`/`fromAll` properties into a [ProjectionsOuterClass.FromEveryDefinition]. */
internal fun buildFromEveryDefinitionFromEntries(properties: Map<String, String>): ProjectionsOuterClass.FromEveryDefinition? {
    if (properties.isEmpty()) return null
    return ProjectionsOuterClass.FromEveryDefinition.newBuilder()
        .putAllProperties(properties)
        .setIncludeChildren(true)
        .build()
}
