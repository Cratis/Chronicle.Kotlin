// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Multiplexed.ObservationMultiplexed
import Cratis.Chronicle.Contracts.Observation.Multiplexed.MultiplexedObservationGrpcKt
import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import Cratis.Chronicle.Contracts.Observation.Reducers.ObservationReducers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Chooses, each time an observer opens its stream, between the shared multiplexed call and the observer's own call.
 *
 * The shared call is used only when the kernel advertised the `multiplexed-observation` capability and
 * multiplexing has not been turned off. A client that skipped the compatibility check never saw the capability, so it
 * observes per observer, exactly as before.
 *
 * @param stub The multiplexed observation service.
 */
internal class MultiplexedObservation(stub: MultiplexedObservationGrpcKt.MultiplexedObservationCoroutineStub) {
    /** Whether multiplexing is allowed at all. On by default. */
    @Volatile
    var enabled: () -> Boolean = { true }

    /** What the kernel advertised. Read on every stream, so a reconnect to a kernel that changed is honored. */
    @Volatile
    var advertisedCapabilities: () -> Set<String> = { emptySet() }

    private val channel = MultiplexedObservationChannel({ requests -> stub.observe(requests) })

    /** Whether new observation streams share one call. */
    val isActive: Boolean get() = enabled() && MULTIPLEXED_OBSERVATION_CAPABILITY in advertisedCapabilities()

    /**
     * Observes a reactor.
     *
     * @param requests What the reactor sends.
     * @param perObserver Opens the reactor's own call, used when the shared call is not.
     */
    fun reactor(
        requests: Flow<ObservationReactors.ReactorMessage>,
        perObserver: (Flow<ObservationReactors.ReactorMessage>) -> Flow<ObservationReactors.EventsToObserve>
    ): Flow<ObservationReactors.EventsToObserve> = flow {
        val source = if (isActive) {
            channel.stream(
                ObservationMultiplexed.MultiplexedObserverKind.Reactor,
                requests,
                { request, message -> message.setReactor(ObservationMultiplexed.ReactorMessage.parseFrom(request.toByteString())) },
                { received ->
                    if (received.hasReactor()) ObservationReactors.EventsToObserve.parseFrom(received.reactor.toByteString()) else null
                }
            )
        } else {
            perObserver(requests)
        }
        source.collect { emit(it) }
    }

    /**
     * Observes a reducer.
     *
     * @param requests What the reducer sends.
     * @param perObserver Opens the reducer's own call, used when the shared call is not.
     */
    fun reducer(
        requests: Flow<ObservationReducers.ReducerMessage>,
        perObserver: (Flow<ObservationReducers.ReducerMessage>) -> Flow<ObservationReducers.ReduceOperationMessage>
    ): Flow<ObservationReducers.ReduceOperationMessage> = flow {
        val source = if (isActive) {
            channel.stream(
                ObservationMultiplexed.MultiplexedObserverKind.Reducer,
                requests,
                { request, message -> message.setReducer(ObservationMultiplexed.ReducerMessage.parseFrom(request.toByteString())) },
                { received ->
                    if (received.hasReducer()) ObservationReducers.ReduceOperationMessage.parseFrom(received.reducer.toByteString()) else null
                }
            )
        } else {
            perObserver(requests)
        }
        source.collect { emit(it) }
    }
}
