// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import Cratis.Chronicle.Contracts.Events.Constraints.ConstraintsGrpcKt
import Cratis.Chronicle.Contracts.Events.Constraints.EventsConstraints
import io.cratis.chronicle.events.EventType
import kotlin.reflect.full.findAnnotation

private fun defaultScope(): EventsConstraints.ConstraintScope =
    EventsConstraints.ConstraintScope.newBuilder().build()

class ConstraintsService(
    private val eventStoreName: String,
    private val stub: ConstraintsGrpcKt.ConstraintsCoroutineStub
) : IConstraintsService {

    override suspend fun register(vararg constraints: Any) {
        val protoConstraints = constraints.mapNotNull { constraint ->
            if (constraint !is IConstraint) return@mapNotNull null
            val ann = constraint::class.findAnnotation<Constraint>() ?: return@mapNotNull null
            val constraintName = ann.id.ifEmpty { constraint::class.simpleName!! }

            val builder = ConstraintBuilder()
            constraint.define(builder)
            val entries = builder.build()

            entries.map { entry ->
                when (entry) {
                    is ConstraintBuilderEntry.UniqueForEntry -> {
                        val eventAnn = entry.eventClass.findAnnotation<EventType>() ?: return@map null
                        val eventTypeId = eventAnn.id.ifEmpty { entry.eventClass.simpleName!! }
                        EventsConstraints.Constraint.newBuilder()
                            .setName(constraintName)
                            .setTypeValue(2) // UniqueEventType = 2
                            .setDefinition(
                                EventsConstraints.OneOf_UniqueConstraintDefinition_UniqueEventTypeConstraintDefinition.newBuilder()
                                    .setValue1(
                                        EventsConstraints.UniqueEventTypeConstraintDefinition.newBuilder()
                                            .setEventTypeId(eventTypeId)
                                            .build()
                                    )
                                    .build()
                            )
                            .setScope(defaultScope())
                            .build()
                    }
                    is ConstraintBuilderEntry.UniqueEntry -> {
                        val eventAnn = entry.eventClass.findAnnotation<EventType>() ?: return@map null
                        val eventTypeId = eventAnn.id.ifEmpty { entry.eventClass.simpleName!! }
                        val eventDef = EventsConstraints.UniqueConstraintEventDefinition.newBuilder()
                            .setEventTypeId(eventTypeId)
                            .addProperties(entry.propertyName)
                            .build()
                        EventsConstraints.Constraint.newBuilder()
                            .setName(constraintName)
                            .setTypeValue(1) // Unique = 1
                            .setDefinition(
                                EventsConstraints.OneOf_UniqueConstraintDefinition_UniqueEventTypeConstraintDefinition.newBuilder()
                                    .setValue0(
                                        EventsConstraints.UniqueConstraintDefinition.newBuilder()
                                            .addEventDefinitions(eventDef)
                                            .setIgnoreCasing(entry.ignoreCasing)
                                            .build()
                                    )
                                    .build()
                            )
                            .setScope(defaultScope())
                            .build()
                    }
                }
            }.filterNotNull()
        }.flatten()

        if (protoConstraints.isEmpty()) return

        val request = EventsConstraints.RegisterConstraintsRequest.newBuilder()
            .setEventStore(eventStoreName)
            .addAllConstraints(protoConstraints)
            .build()

        stub.register(request)
    }
}
