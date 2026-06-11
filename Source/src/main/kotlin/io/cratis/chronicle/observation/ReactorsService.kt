// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import Cratis.Chronicle.Contracts.Observation.Reactors.ReactorsGrpcKt
import com.google.gson.Gson
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeGeneration
import io.cratis.chronicle.events.EventTypeId
import io.cratis.chronicle.identity.Identity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions

private val gson = Gson()

class ReactorsService(
    private val eventStoreName: String,
    private val namespace: String,
    private val connectionId: String,
    private val stub: ReactorsGrpcKt.ReactorsCoroutineStub
) : IReactorsService {

    override suspend fun register(reactor: Any): Job {
        val reactorClass = reactor::class
        val annotation = reactorClass.findAnnotation<Reactor>()
        val reactorId = annotation?.id?.ifEmpty { reactorClass.simpleName!! }
            ?: reactorClass.simpleName!!

        // Map event type ID -> handler function + event KClass
        val handlersByEventTypeId = mutableMapOf<String, Pair<kotlin.reflect.KFunction<*>, KClass<*>>>()
        for (fn in reactorClass.memberFunctions) {
            val params = fn.parameters
            if (params.size < 2) continue
            val eventParam = params[1]
            val eventKClass = eventParam.type.classifier as? KClass<*> ?: continue
            val eventAnnotation = eventKClass.findAnnotation<EventType>() ?: continue
            val eventTypeId = eventAnnotation.id.ifEmpty { eventKClass.simpleName!! }
            handlersByEventTypeId[eventTypeId] = fn to eventKClass
        }

        val eventTypes = handlersByEventTypeId.map { (id, pair) ->
            val (_, eventKClass) = pair
            val ann = eventKClass.findAnnotation<EventType>()!!
            ObservationReactors.EventTypeWithKeyExpression.newBuilder()
                .setEventType(
                    ObservationReactors.EventType.newBuilder()
                        .setId(id)
                        .setGeneration(ann.generation)
                        .build()
                )
                .setKey("EventSourceId")
                .build()
        }

        // Use a Channel instead of MutableSharedFlow so that messages sent before
        // the gRPC stub starts collecting are buffered and not dropped.
        val requests = Channel<ObservationReactors.ReactorMessage>(Channel.BUFFERED)

        return CoroutineScope(Dispatchers.IO).launch {
            try {
            requests.send(
                ObservationReactors.ReactorMessage.newBuilder()
                    .setContent(
                        ObservationReactors.OneOf_RegisterReactor_ReactorResult.newBuilder()
                            .setValue0(
                                ObservationReactors.RegisterReactor.newBuilder()
                                    .setConnectionId(connectionId)
                                    .setEventStore(eventStoreName)
                                    .setNamespace(namespace)
                                    .setReactor(
                                        ObservationReactors.ReactorDefinition.newBuilder()
                                            .setReactorId(reactorId)
                                            .setEventSequenceId(EventSequenceId.eventLog.value)
                                            .setIsReplayable(true)
                                            .addAllEventTypes(eventTypes)
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )

            stub.observe(requests.receiveAsFlow()).collect { eventsToObserve ->
                val partition = eventsToObserve.partition
                val exceptions = mutableListOf<String>()
                var stackTrace = ""
                var lastSuccessfulSequenceNumber = 0L

                for (appendedEvent in eventsToObserve.eventsList) {
                    val eventTypeId = appendedEvent.context.eventType.id
                    val handlerPair = handlersByEventTypeId[eventTypeId]
                    if (handlerPair != null) {
                        val (fn, eventKClass) = handlerPair
                        try {
                            val event = gson.fromJson(appendedEvent.content, eventKClass.java)
                            val ctx = buildEventContext(appendedEvent.context)
                            val params = fn.parameters
                            if (params.size == 3) {
                                fn.call(reactor, event, ctx)
                            } else {
                                fn.call(reactor, event)
                            }
                            lastSuccessfulSequenceNumber = appendedEvent.context.sequenceNumber
                        } catch (e: Exception) {
                            exceptions.add(e.message ?: "Error in ${fn.name}")
                            stackTrace = e.stackTraceToString()
                        }
                    } else {
                        lastSuccessfulSequenceNumber = appendedEvent.context.sequenceNumber
                    }
                }

                val resultState = if (exceptions.isEmpty())
                    ObservationReactors.ObservationState.Success
                else
                    ObservationReactors.ObservationState.Failed

                requests.send(
                    ObservationReactors.ReactorMessage.newBuilder()
                        .setContent(
                            ObservationReactors.OneOf_RegisterReactor_ReactorResult.newBuilder()
                                .setValue1(
                                    ObservationReactors.ReactorResult.newBuilder()
                                        .setPartition(partition)
                                        .setState(resultState)
                                        .setLastSuccessfulObservation(lastSuccessfulSequenceNumber)
                                        .addAllExceptionMessages(exceptions)
                                        .setExceptionStackTrace(stackTrace)
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
            }
            } catch (e: Exception) {
                System.err.println("[ReactorsService] '$reactorId' failed: ${e.message}")
            }
        }
    }

    private fun buildEventContext(ctx: ObservationReactors.EventContext): EventContext {
        val occurred = try {
            Instant.parse(ctx.occurred.value)
        } catch (e: Exception) {
            Instant.now()
        }
        return EventContext(
            sequenceNumber = ctx.sequenceNumber,
            eventSourceId = ctx.eventSourceId,
            eventType = EventTypeDescriptor(
                id = EventTypeId(ctx.eventType.id),
                generation = EventTypeGeneration(ctx.eventType.generation)
            ),
            occurred = occurred,
            correlationId = UUID.randomUUID(),
            causedBy = Identity.unknown
        )
    }
}
