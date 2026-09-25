// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import Cratis.Chronicle.Contracts.Events.Constraints.ConstraintsGrpcKt
import Cratis.Chronicle.Contracts.Events.Constraints.EventsConstraints
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.ConstraintViolation
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * The wire encoding for [ConstraintScope] uses the mere presence of a non-empty string in each
 * `ConstraintScope` field as the "this dimension is scoped" flag - the kernel does not match
 * against the literal value, so any non-empty marker works.
 */
private const val SCOPED_MARKER = "_scoped_"

private fun ConstraintScope?.toContractScope(): EventsConstraints.ConstraintScope =
    EventsConstraints.ConstraintScope.newBuilder().apply {
        if (this@toContractScope?.perEventSourceType == true) eventSourceType = SCOPED_MARKER
        if (this@toContractScope?.perEventStreamType == true) eventStreamType = SCOPED_MARKER
        if (this@toContractScope?.perEventStreamId == true) eventStreamId = SCOPED_MARKER
    }.build()

class ConstraintsService(
    private val eventStoreName: String,
    private val stub: ConstraintsGrpcKt.ConstraintsCoroutineStub
) : IConstraintsService {

    private val messages = ConcurrentHashMap<String, (ConstraintViolation) -> String>()

    internal fun resolveMessageFor(violation: ConstraintViolation): ConstraintViolation {
        var message = messages[violation.constraintId]?.invoke(violation) ?: return violation
        for ((key, value) in violation.details) {
            message = message.replace("{$key}", value)
        }
        return if (message.isEmpty()) violation else violation.copy(message = message)
    }

    override suspend fun register(vararg constraints: Any) {
        val registeredMessages = mutableMapOf<String, (ConstraintViolation) -> String>()
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
                                            .addEventTypeIds(eventTypeId)
                                            .build()
                                    )
                                    .build()
                            )
                            .setScope(entry.scope.toContractScope())
                            .build()
                            .also { registeredMessages[constraintName] = { entry.message } }
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
                            .setScope(entry.scope.toContractScope())
                            .build()
                            .also { registeredMessages[constraintName] = { entry.message } }
                    }
                }
            }.filterNotNull()
        }.flatten()

        sendToKernel(protoConstraints, registeredMessages)
    }

    override suspend fun registerModelBound(eventTypes: List<KClass<*>>) {
        val constraints = ModelBoundConstraints.buildFor(eventTypes)
        val registeredMessages = ModelBoundConstraints.messagesFor(eventTypes)
            .filterKeys { name -> constraints.any { it.name == name } }
            .mapValues { (_, message) -> { _: ConstraintViolation -> message } }
        sendToKernel(constraints, registeredMessages)
    }

    private suspend fun sendToKernel(
        constraints: List<EventsConstraints.Constraint>,
        registeredMessages: Map<String, (ConstraintViolation) -> String>
    ) {
        if (constraints.isEmpty()) return

        val request = EventsConstraints.RegisterConstraintsRequest.newBuilder()
            .setEventStore(eventStoreName)
            .addAllConstraints(constraints)
            .build()

        stub.register(request)
        messages.putAll(registeredMessages)
    }
}
