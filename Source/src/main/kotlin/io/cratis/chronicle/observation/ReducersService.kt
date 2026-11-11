// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Reducers.ObservationReducers
import Cratis.Chronicle.Contracts.Observation.Reducers.ReducersGrpcKt
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.diagnostics.ChronicleTraces
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.EVENT_SOURCE_ID_EXPRESSION
import io.cratis.chronicle.sinks.WellKnownSinkTypes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.reflect.full.findAnnotation
import kotlin.time.Duration

/**
 * Registers reducers with the kernel and keeps their observations established.
 *
 * @param reconnectBackoff Creates the backoff deciding how long each reducer waits before re-establishing its
 * observation.
 * @param waitBeforeReconnect How the wait before a reducer re-establishes its observation is done.
 */
class ReducersService internal constructor(
    private val eventStoreName: String,
    private val namespace: String,
    private val lifecycle: ConnectionLifecycle,
    private val stub: ReducersGrpcKt.ReducersCoroutineStub,
    private val defaultSinkTypeId: String,
    private val readModels: io.cratis.chronicle.readModels.ReadModelsService?,
    private val traces: ChronicleTraces,
    private val reconnectBackoff: () -> ObservationReconnectBackoff,
    private val waitBeforeReconnect: suspend (Duration) -> Unit
) : IReducersService {

    constructor(
        eventStoreName: String,
        namespace: String,
        lifecycle: ConnectionLifecycle,
        stub: ReducersGrpcKt.ReducersCoroutineStub,
        defaultSinkTypeId: String = WellKnownSinkTypes.MONGODB,
        readModels: io.cratis.chronicle.readModels.ReadModelsService? = null,
        traces: ChronicleTraces = ChronicleTraces.default
    ) : this(
        eventStoreName, namespace, lifecycle, stub, defaultSinkTypeId, readModels, traces,
        { ObservationReconnectBackoff() }, { delay(it) }
    )

    override suspend fun register(reducer: Any): Job {
        val registration = ReducerRegistration.from(reducer::class)

        // Auto-register the read model so the observer type (Reducer) and identifier are derived
        // from this reducer rather than from the @ReadModel annotation.
        registration.readModelClass?.let { readModels?.registerWithObserver(it, 1, registration.id, passive = !registration.isActive) }
        readModels?.registerReducer(reducer, registration)

        val dispatch = ReducerEventDispatch(registration, ReducerFold(registration, reducer, traces))

        val eventTypes = registration.eventTypes.map { (id, eventClass) ->
            val ann = eventClass.findAnnotation<EventType>()!!
            ObservationReducers.EventTypeWithKeyExpression.newBuilder()
                .setEventType(
                    ObservationReducers.EventType.newBuilder()
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
                while (currentCoroutineContext().isActive) {
                    backoff.streamOpened()
                    try {
                        doObserve(connectionId, eventTypes, dispatch, registration, backoff)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // gRPC may surface the cancellation of a superseded connection as a CANCELLED
                        // status rather than a CancellationException; that stream did not fail.
                        currentCoroutineContext().ensureActive()
                        System.err.println("[ReducersService] '${registration.id}' failed: ${e.message}")

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

    private suspend fun doObserve(
        connectionId: String,
        eventTypes: List<ObservationReducers.EventTypeWithKeyExpression>,
        dispatch: ReducerEventDispatch,
        registration: ReducerRegistration,
        backoff: ObservationReconnectBackoff
    ) {
        // Use a Channel instead of MutableSharedFlow so that messages sent before
        // the gRPC stub starts collecting are buffered and not dropped.
        val requests = Channel<ObservationReducers.ReducerMessage>(Channel.BUFFERED)
        try {
            requests.send(
                ObservationReducers.ReducerMessage.newBuilder()
                    .setContent(
                        ObservationReducers.OneOf_RegisterReducer_ReducerResult.newBuilder()
                            .setValue0(
                                ObservationReducers.RegisterReducer.newBuilder()
                                    .setConnectionId(connectionId)
                                    .setEventStore(eventStoreName)
                                    .setNamespace(namespace)
                                    .setReducer(
                                        ObservationReducers.ReducerDefinition.newBuilder()
                                            .setReducerId(registration.id)
                                            .setEventSequenceId(registration.eventSequenceId)
                                            .setReadModel(registration.readModelName)
                                            .setIsActive(registration.isActive)
                                            .addAllTags(registration.tags)
                                            .setFilters(
                                                ObservationReducers.ObserverFilters.newBuilder()
                                                    .addAllFilterTags(registration.filters.filterTags)
                                                    .setEventSourceType(registration.filters.eventSourceType)
                                                    .setEventStreamType(registration.filters.eventStreamType)
                                                    .build()
                                            )
                                            .addAllEventTypes(eventTypes)
                                            .build()
                                    )
                                    // Ask for pages: many event sources per delivery, on every path. A
                                    // kernel that does not know the field keeps sending one at a time.
                                    .setPageDelivery(ObservationReducers.PageDelivery.newBuilder().setEnabled(true).build())
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )

            stub.observe(requests.receiveAsFlow()).collect { operation ->
                backoff.streamDelivered()
                // A replay notification on its own carries no events and expects no answer.
                if (operation.isReplayNotification()) return@collect

                val page = operation.toDeliveredPage()
                val context = page.contextFor(registration.id, eventStoreName, namespace, registration.eventSequenceId)
                val outcomes = dispatch.observe(page, context)

                requests.send(
                    ObservationReducers.ReducerMessage.newBuilder()
                        .setContent(
                            ObservationReducers.OneOf_RegisterReducer_ReducerResult.newBuilder()
                                .setValue1(operation.resultFor(outcomes))
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

    private fun ObservationReducers.ReduceOperationMessage.isReplayNotification(): Boolean =
        replayState != ObservationReducers.ReplayState.REPLAY_STATE_None && eventsCount == 0 && partitionsCount == 0
}
