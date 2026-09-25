// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Observation
import Cratis.Chronicle.Contracts.Observation.ObserversGrpcKt
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber

/**
 * Talks to the kernel about the observers registered in an event store.
 *
 * @param eventStoreName The event store the observers belong to.
 * @param namespace The namespace within the event store.
 * @param observers The stub calls are made through.
 */
class Observers(
    private val eventStoreName: String,
    private val namespace: String,
    private val observers: ObserversGrpcKt.ObserversCoroutineStub
) : IObservers {

    override suspend fun getAll(): List<ObserverInformation> {
        val request = Observation.AllObserversRequest.newBuilder().apply {
            this.eventStore = eventStoreName
            this.namespace = this@Observers.namespace
        }.build()

        return observers.getObservers(request).itemsList.map { it.toClient() }
    }

    override suspend fun remove(observerId: String): ObserverRemovalResult {
        val request = Observation.RemoveObserver.newBuilder().apply {
            this.eventStore = eventStoreName
            this.namespace = this@Observers.namespace
            this.observerId = observerId
            this.eventSequenceId = EventSequenceId.eventLog.value
        }.build()

        val response = observers.removeObserver(request)
        return ObserverRemovalResult(response.outcome.toClient(), response.blockingNamespace)
    }

    private fun Observation.ObserverInformation.toClient() = ObserverInformation(
        id = id,
        eventSequenceId = EventSequenceId(eventSequenceId),
        type = type.toClient(),
        runningState = runningState.toClient(),
        lastHandledEventSequenceNumber = EventSequenceNumber(lastHandledEventSequenceNumber),
        nextEventSequenceNumber = EventSequenceNumber(nextEventSequenceNumber),
        handledEventCount = handledEventCount
    )

    private fun Observation.ObserverType.toClient() = when (this) {
        Observation.ObserverType.Reactor -> ObserverType.Reactor
        Observation.ObserverType.Projection -> ObserverType.Projection
        Observation.ObserverType.Reducer -> ObserverType.Reducer
        Observation.ObserverType.External -> ObserverType.External
        else -> ObserverType.Unknown
    }

    private fun Observation.ObserverRunningState.toClient() = when (this) {
        Observation.ObserverRunningState.Active -> ObserverRunningState.Active
        Observation.ObserverRunningState.Suspended -> ObserverRunningState.Suspended
        Observation.ObserverRunningState.Replaying -> ObserverRunningState.Replaying
        Observation.ObserverRunningState.OBSERVER_RUNNING_STATE_Disconnected -> ObserverRunningState.Disconnected
        Observation.ObserverRunningState.Quarantined -> ObserverRunningState.Quarantined
        else -> ObserverRunningState.Unknown
    }

    private fun Observation.ObserverRemovalOutcome.toClient() = when (this) {
        Observation.ObserverRemovalOutcome.Removed -> ObserverRemovalOutcome.Removed
        Observation.ObserverRemovalOutcome.ObserverNotFound -> ObserverRemovalOutcome.ObserverNotFound
        Observation.ObserverRemovalOutcome.ObserverActive -> ObserverRemovalOutcome.ObserverActive
        Observation.ObserverRemovalOutcome.ObserverSubscribed -> ObserverRemovalOutcome.ObserverSubscribed
        else -> throw IllegalArgumentException("Unknown observer removal outcome: $this")
    }
}
