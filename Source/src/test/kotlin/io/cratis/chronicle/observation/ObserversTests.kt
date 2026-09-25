// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Observation
import Cratis.Chronicle.Contracts.Observation.ObserversGrpcKt
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Removal exists for the observer whose declaring code is gone - a deleted read model and its
 * projection, a removed reactor. These pin how listing and removing an observer read off, and map
 * onto, the wire.
 */
class ObserversTests {

    private fun observersFor(
        stub: ObserversGrpcKt.ObserversCoroutineStub = mockk()
    ) = Observers("my-store", "default", stub)

    @Test
    fun `every field on an observer comes across`() = runBlocking {
        val stub = mockk<ObserversGrpcKt.ObserversCoroutineStub>()
        coEvery { stub.getObservers(any(), any()) } returns Observation.IEnumerable_ObserverInformation.newBuilder()
            .addItems(
                Observation.ObserverInformation.newBuilder()
                    .setId("employee-alerts")
                    .setEventSequenceId("event-log")
                    .setType(Observation.ObserverType.Reactor)
                    .setRunningState(Observation.ObserverRunningState.Active)
                    .setLastHandledEventSequenceNumber(6L)
                    .setNextEventSequenceNumber(7L)
                    .setHandledEventCount(6L)
            )
            .build()

        val observer = observersFor(stub).getAll().single()

        assertEquals("employee-alerts", observer.id)
        assertEquals(EventSequenceId.eventLog, observer.eventSequenceId)
        assertEquals(ObserverType.Reactor, observer.type)
        assertEquals(ObserverRunningState.Active, observer.runningState)
        assertEquals(6L, observer.lastHandledEventSequenceNumber.value)
        assertEquals(7L, observer.nextEventSequenceNumber.value)
        assertEquals(6L, observer.handledEventCount)
    }

    @Test
    fun `an event store with no observers lists none`() = runBlocking {
        val stub = mockk<ObserversGrpcKt.ObserversCoroutineStub>()
        coEvery { stub.getObservers(any(), any()) } returns Observation.IEnumerable_ObserverInformation.newBuilder().build()

        assertTrue(observersFor(stub).getAll().isEmpty())
    }

    @Test
    fun `listing asks the event store it was given for`() = runBlocking {
        val stub = mockk<ObserversGrpcKt.ObserversCoroutineStub>()
        val request = slot<Observation.AllObserversRequest>()
        coEvery { stub.getObservers(capture(request), any()) } returns Observation.IEnumerable_ObserverInformation.newBuilder().build()

        observersFor(stub).getAll()

        assertEquals("my-store", request.captured.eventStore)
        assertEquals("default", request.captured.namespace)
    }

    /**
     * `OBSERVER_RUNNING_STATE_Disconnected` is how the wire enum actually names it - protoc prefixes the
     * literal to dodge a collision elsewhere in the file - so a mapping that used the plain name would fail
     * to compile, but a mapping onto the wrong constant would compile fine and misreport silently.
     */
    @Test
    fun `a disconnected observer is reported as disconnected, not unknown`() = runBlocking {
        val stub = mockk<ObserversGrpcKt.ObserversCoroutineStub>()
        coEvery { stub.getObservers(any(), any()) } returns Observation.IEnumerable_ObserverInformation.newBuilder()
            .addItems(
                Observation.ObserverInformation.newBuilder()
                    .setId("employee-alerts")
                    .setEventSequenceId("event-log")
                    .setType(Observation.ObserverType.Reactor)
                    .setRunningState(Observation.ObserverRunningState.OBSERVER_RUNNING_STATE_Disconnected)
            )
            .build()

        assertEquals(ObserverRunningState.Disconnected, observersFor(stub).getAll().single().runningState)
    }

    @Test
    fun `removing names the observer, the event store and the event log`() = runBlocking {
        val stub = mockk<ObserversGrpcKt.ObserversCoroutineStub>()
        val request = slot<Observation.RemoveObserver>()
        coEvery { stub.removeObserver(capture(request), any()) } returns Observation.RemoveObserverResponse.newBuilder()
            .setOutcome(Observation.ObserverRemovalOutcome.Removed)
            .build()

        observersFor(stub).remove("employee-alerts")

        assertEquals("employee-alerts", request.captured.observerId)
        assertEquals("my-store", request.captured.eventStore)
        assertEquals("default", request.captured.namespace)
        assertEquals(EventSequenceId.eventLog.value, request.captured.eventSequenceId)
    }

    @Test
    fun `a removed observer reports itself as removed`() = runBlocking {
        val stub = mockk<ObserversGrpcKt.ObserversCoroutineStub>()
        coEvery { stub.removeObserver(any(), any()) } returns Observation.RemoveObserverResponse.newBuilder()
            .setOutcome(Observation.ObserverRemovalOutcome.Removed)
            .build()

        assertTrue(observersFor(stub).remove("employee-alerts").isRemoved)
    }

    @Test
    fun `an active observer refuses removal and names the namespace still running it`() = runBlocking {
        val stub = mockk<ObserversGrpcKt.ObserversCoroutineStub>()
        coEvery { stub.removeObserver(any(), any()) } returns Observation.RemoveObserverResponse.newBuilder()
            .setOutcome(Observation.ObserverRemovalOutcome.ObserverActive)
            .setBlockingNamespace("production")
            .build()

        val result = observersFor(stub).remove("employee-alerts")

        assertFalse(result.isRemoved)
        assertEquals(ObserverRemovalOutcome.ObserverActive, result.outcome)
        assertEquals("production", result.blockingNamespace)
    }
}
