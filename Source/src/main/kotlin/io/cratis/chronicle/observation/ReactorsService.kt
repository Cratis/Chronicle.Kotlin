// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import Cratis.Chronicle.Contracts.Observation.Reactors.ReactorsGrpcKt
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.diagnostics.ChronicleTraces
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.EVENT_SOURCE_ID_EXPRESSION
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.reflect.full.findAnnotation
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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

    /** Creates the backoff deciding how long each reactor waits before re-establishing its observation. */
    internal var reconnectBackoff: () -> ObservationReconnectBackoff =
        { ObservationReconnectBackoff(baseDelay = REOBSERVE_DELAY_MS.milliseconds) }

    /** How the wait before a reactor re-establishes its observation is done. Specs replace this to observe the waits. */
    internal var waitBeforeReconnect: suspend (Duration) -> Unit = { delay(it) }

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
                .setKey(EVENT_SOURCE_ID_EXPRESSION)
                .build()
        }

        // Re-register on every connection, not just the first. The kernel keys a
        // subscription by connection id and drops it when the client is evicted, so an
        // observation established under an earlier connection is dead once we reconnect.
        val backoff = reconnectBackoff()
        return CoroutineScope(Dispatchers.IO).launch {
            lifecycle.connections().collectLatest { connectionId ->
                while (isActive) {
                    backoff.streamOpened()
                    try {
                        observe(registration, dispatch, connectionId, eventTypes, backoff)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        System.err.println("[ReactorsService] '${registration.id}' failed: ${e.message}")

                        // The kernel no longer knows this connection. Retrying the stream under the same
                        // connection id can never work, so reconnect the whole client, which brings a new
                        // connection id and re-establishes every observation.
                        if (e.isSessionLost()) {
                            lifecycle.requestReconnect(connectionId)
                            awaitCancellation()
                        }
                    }

                    // The kernel closes a cross-store (inbox) stream rather than tailing it
                    // forever, so a stream that ends cleanly still has to be re-established.
                    // Streams that keep ending without working back off, with jitter.
                    waitBeforeReconnect(backoff.nextDelay())
                }
            }
        }
    }

    private suspend fun observe(
        registration: ReactorRegistration,
        dispatch: ReactorEventDispatch,
        connectionId: String,
        eventTypes: List<ObservationReactors.EventTypeWithKeyExpression>,
        backoff: ObservationReconnectBackoff
    ) {
        // Use a Channel instead of MutableSharedFlow so that messages sent before
        // the gRPC stub starts collecting are buffered and not dropped.
        val requests = Channel<ObservationReactors.ReactorMessage>(Channel.BUFFERED)

        try {
            requests.send(registrationMessage(registration, connectionId, eventTypes))

            stub.observe(requests.receiveAsFlow()).collect { eventsToObserve ->
                backoff.streamDelivered()
                // A replay notification on its own carries no events and expects no answer: this client
                // learns of a replay from the flags on the replayed events themselves.
                if (eventsToObserve.isReplayNotification()) return@collect

                val page = eventsToObserve.toDeliveredPage()
                val context = page.contextFor(registration.id, eventStoreName, namespace, registration.eventSequenceId)
                val outcomes = dispatch.observe(page, context)
                requests.send(resultMessage(eventsToObserve.resultFor(outcomes)))
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
                                    // Proto3 omits false; the kernel defaults to true, so send the companion (ignored by older kernels).
                                    .setIsNotReplayable(!registration.isReplayable)
                                    .setStartAtTail(registration.startAtTail)
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
                            // Ask for pages: many partitions per delivery, on every path. A kernel that
                            // does not know the field keeps sending one partition at a time.
                            .setPageDelivery(ObservationReactors.PageDelivery.newBuilder().setEnabled(true).build())
                            .build()
                    )
                    .build()
            )
            .build()

    private fun resultMessage(result: ObservationReactors.ReactorResult): ObservationReactors.ReactorMessage =
        ObservationReactors.ReactorMessage.newBuilder()
            .setContent(
                ObservationReactors.OneOf_RegisterReactor_ReactorResult.newBuilder()
                    .setValue1(result)
                    .build()
            )
            .build()

    private fun ObservationReactors.EventsToObserve.isReplayNotification(): Boolean =
        replayState != ObservationReactors.ReplayState.REPLAY_STATE_None && eventsCount == 0 && partitionsCount == 0

    private companion object {
        /**
         * How long to wait before re-establishing an observation whose stream ended, once the stream had worked.
         * Streams that end without having worked back off from this, see [ObservationReconnectBackoff].
         */
        const val REOBSERVE_DELAY_MS = 2_000L
    }
}
