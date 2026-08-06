// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import Cratis.Chronicle.Contracts.Observation.Reactors.ReactorsGrpcKt
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.diagnostics.ChronicleTraces
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

class ReactorsService(
    private val eventStoreName: String,
    private val namespace: String,
    private val lifecycle: ConnectionLifecycle,
    private val stub: ReactorsGrpcKt.ReactorsCoroutineStub,
    eventLog: IEventLog,
    private val middlewares: ReactorMiddlewares = ReactorMiddlewares.none,
    private val arguments: ReactorMethodArguments = ReactorMethodArguments.contextOnly,
    private val traces: ChronicleTraces = ChronicleTraces.default
) : IReactorsService {

    private val sideEffects = ReactorSideEffects(eventLog)

    override suspend fun register(reactor: Any): Job {
        val registration = ReactorRegistration.from(reactor::class, arguments)
        val dispatch = ReactorEventDispatch(registration, reactor, middlewares, arguments, sideEffects, traces)

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
                        observe(registration, dispatch, connectionId, eventTypes)
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
        dispatch: ReactorEventDispatch,
        connectionId: String,
        eventTypes: List<ObservationReactors.EventTypeWithKeyExpression>
    ) {
        // Use a Channel instead of MutableSharedFlow so that messages sent before
        // the gRPC stub starts collecting are buffered and not dropped.
        val requests = Channel<ObservationReactors.ReactorMessage>(Channel.BUFFERED)

        try {
            requests.send(registrationMessage(registration, connectionId, eventTypes))

            stub.observe(requests.receiveAsFlow()).collect { eventsToObserve ->
                val outcome = dispatch.observe(eventsToObserve.partition, eventsToObserve.eventsList)
                requests.send(resultMessage(eventsToObserve.partition, outcome))
            }
        } finally {
            // Leaking the channel would strand a dead stream holding buffered messages
            // every time an observation is re-established.
            requests.close()
        }
    }

    private fun registrationMessage(
        registration: ReactorRegistration,
        connectionId: String,
        eventTypes: List<ObservationReactors.EventTypeWithKeyExpression>
    ): ObservationReactors.ReactorMessage =
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

    private fun resultMessage(
        partition: String,
        outcome: ReactorObservationOutcome
    ): ObservationReactors.ReactorMessage =
        ObservationReactors.ReactorMessage.newBuilder()
            .setContent(
                ObservationReactors.OneOf_RegisterReactor_ReactorResult.newBuilder()
                    .setValue1(
                        ObservationReactors.ReactorResult.newBuilder()
                            .setPartition(partition)
                            .setState(
                                if (outcome.isSuccess) {
                                    ObservationReactors.ObservationState.Success
                                } else {
                                    ObservationReactors.ObservationState.Failed
                                }
                            )
                            .setLastSuccessfulObservation(outcome.lastSuccessfulSequenceNumber)
                            .addAllExceptionMessages(outcome.exceptions)
                            .setExceptionStackTrace(outcome.stackTrace)
                            .build()
                    )
                    .build()
            )
            .build()

    private companion object {
        /** How long to wait before re-establishing an observation whose stream ended. */
        const val REOBSERVE_DELAY_MS = 2_000L
    }
}
