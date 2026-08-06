// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import Cratis.Chronicle.Contracts.Observation.Reactors.ReactorsGrpcKt
import com.google.gson.Gson
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventType
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
import kotlin.reflect.full.findAnnotation

private val gson = Gson()

class ReactorsService(
    private val eventStoreName: String,
    private val namespace: String,
    private val lifecycle: ConnectionLifecycle,
    private val stub: ReactorsGrpcKt.ReactorsCoroutineStub,
    private val eventLog: IEventLog
) : IReactorsService {

    override suspend fun register(reactor: Any): Job {
        val registration = ReactorRegistration.from(reactor::class)

        val eventTypes = registration.handlers.eventTypes.map { (id, eventKClass) ->
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
                        observe(registration, reactor, connectionId, eventTypes)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        System.err.println("[ReactorsService] '${registration.id}' failed: ${e.message}")
                    }

                    // The kernel closes a cross-store (inbox) stream rather than tailing it
                    // forever, so a stream that ends cleanly still has to be re-established.
                    delay(REOBSERVE_DELAY_MS)
                }
            }
        }
    }

    private suspend fun observe(
        registration: ReactorRegistration,
        reactor: Any,
        connectionId: String,
        eventTypes: List<ObservationReactors.EventTypeWithKeyExpression>
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
                                            .setReactorId(registration.id)
                                            .setEventSequenceId(registration.eventSequenceId)
                                            .setIsReplayable(registration.isReplayable)
                                            .addAllTags(registration.tags)
                                            .setFilters(
                                                ObservationReactors.ObserverFilters.newBuilder()
                                                    .addAllFilterTags(registration.filters.filterTags)
                                                    .setEventSourceType(registration.filters.eventSourceType)
                                                    .setEventStreamType(registration.filters.eventStreamType)
                                                    .build()
                                            )
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
                    val context = appendedEvent.context.toEventContext()
                    val resolution =
                        registration.handlers.resolve(context.eventType.id.value, context.observationState)

                    if (resolution !is ReactorHandlerResolution.Invoke) {
                        // A handler deliberately skipped for replay and an event no handler wants
                        // are both fully observed - the reactor is caught up past them either way.
                        // A skipped @OnceOnly handler produces no side effects either, which is the
                        // whole point of taking it out of the replay.
                        lastSuccessfulSequenceNumber = context.sequenceNumber
                        continue
                    }

                    val handler = resolution.handler
                    try {
                        val event = gson.fromJson(appendedEvent.content, handler.eventClass.java)
                        val result = if (handler.function.parameters.size == 3) {
                            handler.function.call(reactor, event, context)
                        } else {
                            handler.function.call(reactor, event)
                        }
                        appendSideEffects(result, context.eventSourceId)
                        lastSuccessfulSequenceNumber = context.sequenceNumber
                    } catch (e: Exception) {
                        exceptions.add(e.message ?: "Error in ${handler.function.name}")
                        stackTrace = e.stackTraceToString()
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
            is EventForEventSourceId -> appendIfEventType(result)
            is List<*> -> result.filterNotNull().forEach { item ->
                if (item is EventForEventSourceId) {
                    appendIfEventType(item)
                } else {
                    appendIfEventType(EventForEventSourceId(triggeringEventSourceId, item))
                }
            }
            else -> appendIfEventType(EventForEventSourceId(triggeringEventSourceId, result))
        }
    }

    /**
     * Appends the event when its class carries `@EventType`; silently ignores anything else.
     *
     * The shaping the caller put on [EventForEventSourceId] is carried through, so a side effect can
     * target a stream, carry tags, or name a subject exactly as a direct append can. Dropping it
     * here would make those fields silently do nothing on this path.
     */
    private suspend fun appendIfEventType(sideEffect: EventForEventSourceId) {
        val event = sideEffect.event
        if (event::class.findAnnotation<EventType>() == null) return
        val result = eventLog.append(sideEffect.eventSourceId, event, sideEffect.toAppendOptions())
        if (!result.isSuccess) {
            val messages = result.errors.joinToString { it.message }.ifEmpty { "constraint violation" }
            throw IllegalStateException("Failed to append reactor side-effect event '${event::class.simpleName}': $messages")
        }
    }

    private companion object {
        /** How long to wait before re-establishing an observation whose stream ended. */
        const val REOBSERVE_DELAY_MS = 2_000L
    }
}
