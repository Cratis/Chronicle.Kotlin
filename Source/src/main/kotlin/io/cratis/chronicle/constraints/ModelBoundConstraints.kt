// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import Cratis.Chronicle.Contracts.Events.Constraints.EventsConstraints
import io.cratis.chronicle.events.EventType
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * Builds the wire [EventsConstraints.Constraint] messages for every [Unique] and [RemoveConstraint]
 * declared directly on a set of event types - the model-bound alternative to a hand-written
 * [IConstraint].
 */
internal object ModelBoundConstraints {

    /**
     * Builds a [EventsConstraints.Constraint] per distinct constraint name found across [eventTypes]
     * - one per class-level [Unique], and one per group of property-level [Unique] annotations that
     * share a name.
     */
    fun buildFor(eventTypes: List<KClass<*>>): List<EventsConstraints.Constraint> {
        val removedWith = removedWithByConstraintName(eventTypes)
        return classLevelConstraints(eventTypes, removedWith) + propertyLevelConstraints(eventTypes, removedWith)
    }

    /**
     * The event type id releasing each constraint name, keeping only the first one found.
     *
     * The wire message this client targets carries a single releasing event type per constraint, not
     * a set of them - see [RemoveConstraint]'s documentation. A name with more than one candidate is
     * reported so the gap is visible rather than silently resolved.
     */
    private fun removedWithByConstraintName(eventTypes: List<KClass<*>>): Map<String, String> {
        val byName = mutableMapOf<String, MutableList<KClass<*>>>()
        for (eventType in eventTypes) {
            for (annotation in eventType.java.getAnnotationsByType(RemoveConstraint::class.java)) {
                byName.getOrPut(annotation.value) { mutableListOf() }.add(eventType)
            }
        }
        for ((name, releasers) in byName) {
            if (releasers.size > 1) {
                System.err.println(
                    "[ModelBoundConstraints] Constraint '$name' is released by ${releasers.size} event types " +
                        "(${releasers.joinToString { it.simpleName ?: it.toString() }}); only " +
                        "'${releasers.first().simpleName}' will actually release it - this client's contracts " +
                        "version can express only one releasing event type per constraint."
                )
            }
        }
        return byName.mapValues { (_, releasers) -> eventTypeIdOf(releasers.first()) }
    }

    private fun eventTypeIdOf(eventType: KClass<*>): String =
        eventType.findAnnotation<EventType>()?.id?.ifEmpty { eventType.simpleName!! } ?: eventType.simpleName!!

    private fun classLevelConstraints(
        eventTypes: List<KClass<*>>,
        removedWith: Map<String, String>
    ): List<EventsConstraints.Constraint> = eventTypes.mapNotNull { eventType ->
        val unique = eventType.findAnnotation<Unique>() ?: return@mapNotNull null
        val name = unique.id.ifEmpty { eventType.simpleName!! }

        constraintBuilder(name, removedWith)
            .setTypeValue(2) // UniqueEventType
            .setDefinition(
                EventsConstraints.OneOf_UniqueConstraintDefinition_UniqueEventTypeConstraintDefinition.newBuilder()
                    .setValue1(
                        EventsConstraints.UniqueEventTypeConstraintDefinition.newBuilder()
                            .addEventTypeIds(eventTypeIdOf(eventType))
                            .build()
                    )
                    .build()
            )
            .build()
    }

    private fun propertyLevelConstraints(
        eventTypes: List<KClass<*>>,
        removedWith: Map<String, String>
    ): List<EventsConstraints.Constraint> {
        val marked = eventTypes.flatMap { eventType ->
            eventType.memberProperties.mapNotNull { property ->
                property.findAnnotation<Unique>()?.let { Triple(eventType, property.name, it) }
            }
        }

        return marked
            .groupBy { (_, propertyName, unique) -> unique.id.ifEmpty { propertyName } }
            .map { (name, group) ->
                val eventDefinitions = group.map { (eventType, propertyName, _) ->
                    EventsConstraints.UniqueConstraintEventDefinition.newBuilder()
                        .setEventTypeId(eventTypeIdOf(eventType))
                        .addProperties(propertyName)
                        .build()
                }

                constraintBuilder(name, removedWith)
                    .setTypeValue(1) // Unique
                    .setDefinition(
                        EventsConstraints.OneOf_UniqueConstraintDefinition_UniqueEventTypeConstraintDefinition.newBuilder()
                            .setValue0(
                                EventsConstraints.UniqueConstraintDefinition.newBuilder()
                                    .addAllEventDefinitions(eventDefinitions)
                                    .setIgnoreCasing(false)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            }
    }

    private fun constraintBuilder(name: String, removedWith: Map<String, String>): EventsConstraints.Constraint.Builder {
        val builder = EventsConstraints.Constraint.newBuilder()
            .setName(name)
            .setScope(EventsConstraints.ConstraintScope.newBuilder().build())
        // The contract carries several removal event types per constraint; a model-bound constraint
        // declares at most one, so there is exactly one to add.
        removedWith[name]?.let { builder.addRemovedWith(it) }
        return builder
    }
}
