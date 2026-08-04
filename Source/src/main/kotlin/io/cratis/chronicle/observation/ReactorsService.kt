// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import Cratis.Chronicle.Contracts.Observation.Reactors.ReactorsGrpcKt
import com.google.gson.Gson
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeGeneration
import io.cratis.chronicle.events.EventTypeId
import io.cratis.chronicle.identity.Identity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
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
    private val lifecycle: ConnectionLifecycle,
    private val stub: ReactorsGrpcKt.ReactorsCoroutineStub,
    private val eventLog: IEventLog
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

        // Re-register on every connection, not just the first. The kernel keys a
        // subscription by connection id and drops it when the client is evicted, so an
        // observation established under an earlier connection is dead once we reconnect.
        return CoroutineScope(Dispatchers.IO).launch {
            lifecycle.connections().collectLatest { connectionId ->
                while (isActive) {
                    try {
                        observe(reactorId, reactor, connectionId, eventTypes, handlersByEventTypeId)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        System.err.println("[ReactorsService] '$reactorId' failed: ${e.message}")
                    }

                    // The kernel closes a cross-store (inbox) stream rather than tailing it
                    // forever, so a stream that ends cleanly still has to be re-established.
                    delay(REOBSERVE_DELAY_MS)
                }
            }
        }
    }

    private suspend fun observe(
        reactorId: String,
        reactor: Any,
        connectionId: String,
        eventTypes: List<ObservationReactors.EventTypeWithKeyExpression>,
        handlersByEventTypeId: Map<String, Pair<kotlin.reflect.KFunction<*>, KClass<*>>>
    ) {
        // Use a Channel instead of MutableSharedFlow so that messages sent before
        // the gRPC stub starts collecting are buffered and not dropped.
        val requests = Channel<ObservationReactors.ReactorMessage>(Channel.BUFFERED)

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
                            val result = if (params.size == 3) {
                                fn.call(reactor, event, ctx)
                            } else {
                                fn.call(reactor, event)
                            }
                            appendSideEffects(result, appendedEvent.context.eventSourceId)
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
        } finally {
            // Leaking the channel would strand a dead stream holding buffered messages
            // every time an observation is re-established.
            requests.close()
        }
    }

    /**
     * Auto-appends whatever a reactor handler method returned, mirroring the C# side-effect
     * conventions: a single event object or [EventForEventSourceId] append directly, and a `List`
     * may freely mix bare events (appended to [triggeringEventSourceId]) with
     * [EventForEventSourceId] wrappers (appended to their own, explicit event source id).
     * Non-event return values (`Unit`, `null`, anything not annotated with `@EventType`) are ignored.
     */
    private suspend fun appendSideEffects(result: Any?, triggeringEventSourceId: String) {
        when (result) {
            null, Unit -> return
            is EventForEventSourceId -> appendIfEventType(result.eventSourceId, result.event)
            is List<*> -> result.filterNotNull().forEach { item ->
                if (item is EventForEventSourceId) {
                    appendIfEventType(item.eventSourceId, item.event)
                } else {
                    appendIfEventType(triggeringEventSourceId, item)
                }
            }
            else -> appendIfEventType(triggeringEventSourceId, result)
        }
    }

    /** Appends [event] to [eventSourceId] when its class carries `@EventType`; silently ignores anything else. */
    private suspend fun appendIfEventType(eventSourceId: String, event: Any) {
        if (event::class.findAnnotation<EventType>() == null) return
        val result = eventLog.append(eventSourceId, event)
        if (!result.isSuccess) {
            val messages = result.errors.joinToString { it.message }.ifEmpty { "constraint violation" }
            throw IllegalStateException("Failed to append reactor side-effect event '${event::class.simpleName}': $messages")
        }
    }

    private companion object {
        /** How long to wait before re-establishing an observation whose stream ended. */
        const val REOBSERVE_DELAY_MS = 2_000L
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
