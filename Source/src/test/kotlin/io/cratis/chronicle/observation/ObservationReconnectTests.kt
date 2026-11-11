// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import Cratis.Chronicle.Contracts.Observation.Reactors.ReactorsGrpcKt
import Cratis.Chronicle.Contracts.Observation.Reducers.ObservationReducers
import Cratis.Chronicle.Contracts.Observation.Reducers.ReducersGrpcKt
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.eventSequences.IEventLog
import io.grpc.Status
import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * A reactor or reducer whose observation stream keeps ending backs off before re-establishing it, and one the kernel
 * no longer knows the connection of has the whole client reconnect rather than retrying under the stale id.
 *
 * The waits are recorded instead of slept, and the jitter is fixed, so every wait is exact.
 */
class ObservationReconnectTests {
    @Reactor("reconnecting-reactor")
    class ReconnectingReactor {
        fun opened(@Suppress("UNUSED_PARAMETER") event: AccountOpened) = Unit
    }

    @Reducer("reconnecting-reducer")
    class ReconnectingReducer {
        @Observes(AccountOpened::class)
        fun reduce(events: EventBatch, @Suppress("UNUSED_PARAMETER") current: AccountBalance?): AccountBalance =
            AccountBalance((events.last().event as AccountOpened).owner)
    }

    private val clientIsNotConnected =
        Status.UNKNOWN.withDescription("Exception was thrown by handler. ClientIsNotConnected: Client 'stale' is not connected.")

    private fun fixedBackoff() = ObservationReconnectBackoff(
        baseDelay = 2.seconds,
        maxDelay = 60.seconds,
        healthyStreamDuration = 3.minutes,
        currentTimeMillis = { 0L },
        nextJitter = { 1.0 }
    )

    /** Records each wait; once [count] have been recorded, completes [done] and holds the loop still. */
    private class RecordedWaits(private val count: Int) {
        val waits = CopyOnWriteArrayList<Duration>()
        val done = CompletableDeferred<Unit>()

        val wait: suspend (Duration) -> Unit = { delay ->
            waits += delay
            if (waits.size >= count) {
                done.complete(Unit)
                awaitCancellation()
            }
        }
    }

    private val replayNotification: ObservationReactors.EventsToObserve = ObservationReactors.EventsToObserve.newBuilder()
        .setReplayState(
            ObservationReactors.ReplayState.entries.first {
                it != ObservationReactors.ReplayState.REPLAY_STATE_None && it != ObservationReactors.ReplayState.UNRECOGNIZED
            }
        )
        .build()

    private val reducerReplayNotification: ObservationReducers.ReduceOperationMessage =
        ObservationReducers.ReduceOperationMessage.newBuilder()
            .setReplayState(
                ObservationReducers.ReplayState.entries.first {
                    it != ObservationReducers.ReplayState.REPLAY_STATE_None && it != ObservationReducers.ReplayState.UNRECOGNIZED
                }
            )
            .build()

    private fun reactorsService(lifecycle: ConnectionLifecycle, stub: ReactorsGrpcKt.ReactorsCoroutineStub, recorded: RecordedWaits) =
        ReactorsService("store", "default", lifecycle, stub, mockk<IEventLog>()).apply {
            reconnectBackoff = ::fixedBackoff
            waitBeforeReconnect = recorded.wait
        }

    private fun reducersService(lifecycle: ConnectionLifecycle, stub: ReducersGrpcKt.ReducersCoroutineStub, recorded: RecordedWaits) =
        ReducersService("store", "default", lifecycle, stub).apply {
            reconnectBackoff = ::fixedBackoff
            waitBeforeReconnect = recorded.wait
        }

    @Test
    fun `a reactor whose streams keep ending without working waits longer each time`() = runBlocking {
        val lifecycle = ConnectionLifecycle().apply { markConnected(connectionId) }
        val stub = mockk<ReactorsGrpcKt.ReactorsCoroutineStub>()
        every { stub.observe(any(), any()) } answers { flow { } }
        val recorded = RecordedWaits(4)

        val job = reactorsService(lifecycle, stub, recorded).register(ReconnectingReactor())
        try {
            withTimeout(5_000) { recorded.done.await() }
        } finally {
            job.cancel()
        }

        assertEquals(listOf(2, 4, 8, 16).map { it.seconds }, recorded.waits)
    }

    @Test
    fun `a reactor whose streams deliver before they end waits the base delay each time`() = runBlocking {
        val lifecycle = ConnectionLifecycle().apply { markConnected(connectionId) }
        val stub = mockk<ReactorsGrpcKt.ReactorsCoroutineStub>()
        every { stub.observe(any(), any()) } answers { flow { emit(replayNotification) } }
        val recorded = RecordedWaits(4)

        val job = reactorsService(lifecycle, stub, recorded).register(ReconnectingReactor())
        try {
            withTimeout(5_000) { recorded.done.await() }
        } finally {
            job.cancel()
        }

        assertEquals(List(4) { 2.seconds }, recorded.waits)
    }

    @Test
    fun `a reducer whose streams keep ending without working waits longer each time`() = runBlocking {
        val lifecycle = ConnectionLifecycle().apply { markConnected(connectionId) }
        val stub = mockk<ReducersGrpcKt.ReducersCoroutineStub>()
        every { stub.observe(any(), any()) } answers { flow { } }
        val recorded = RecordedWaits(4)

        val job = reducersService(lifecycle, stub, recorded).register(ReconnectingReducer())
        try {
            withTimeout(5_000) { recorded.done.await() }
        } finally {
            job.cancel()
        }

        assertEquals(listOf(2, 4, 8, 16).map { it.seconds }, recorded.waits)
    }

    @Test
    fun `a reducer whose streams deliver before they end waits the base delay each time`() = runBlocking {
        val lifecycle = ConnectionLifecycle().apply { markConnected(connectionId) }
        val stub = mockk<ReducersGrpcKt.ReducersCoroutineStub>()
        every { stub.observe(any(), any()) } answers { flow { emit(reducerReplayNotification) } }
        val recorded = RecordedWaits(4)

        val job = reducersService(lifecycle, stub, recorded).register(ReconnectingReducer())
        try {
            withTimeout(5_000) { recorded.done.await() }
        } finally {
            job.cancel()
        }

        assertEquals(List(4) { 2.seconds }, recorded.waits)
    }

    @Test
    fun `a reactor the kernel no longer knows the connection of has the client reconnect under a new connection id`() = runBlocking {
        val lifecycle = ConnectionLifecycle().apply { markConnected(connectionId) }
        val stale = lifecycle.connectionId
        val stub = mockk<ReactorsGrpcKt.ReactorsCoroutineStub>()
        val observeCalls = AtomicInteger()
        val reRegistered = CompletableDeferred<ObservationReactors.ReactorMessage>()
        every { stub.observe(any(), any()) } answers {
            val requests = firstArg<Flow<ObservationReactors.ReactorMessage>>()
            if (observeCalls.incrementAndGet() == 1) {
                flow { throw clientIsNotConnected.asException() }
            } else {
                flow {
                    launch { reRegistered.complete(requests.first()) }
                    awaitCancellation()
                }
            }
        }
        val recorded = RecordedWaits(1)
        val reconnectRequest = async(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) { lifecycle.reconnectRequests.first() }

        val job = reactorsService(lifecycle, stub, recorded).register(ReconnectingReactor())
        try {
            assertEquals(stale, withTimeout(5_000) { reconnectRequest.await() })

            // What the connection manager does with the request: a full reconnect under a new connection id.
            lifecycle.markDisconnected()
            lifecycle.markConnected(lifecycle.connectionId)
            val registration = withTimeout(5_000) { reRegistered.await() }.content.value0

            assertNotEquals(stale, registration.connectionId)
            assertEquals(lifecycle.connectionId, registration.connectionId)
            assertEquals(2, observeCalls.get())
            assertTrue(recorded.waits.isEmpty(), "the stale stream must not be retried, but it waited ${recorded.waits}")
        } finally {
            job.cancel()
        }
    }

    @Test
    fun `a reducer the kernel no longer knows the connection of has the client reconnect under a new connection id`() = runBlocking {
        val lifecycle = ConnectionLifecycle().apply { markConnected(connectionId) }
        val stale = lifecycle.connectionId
        val stub = mockk<ReducersGrpcKt.ReducersCoroutineStub>()
        val observeCalls = AtomicInteger()
        val reRegistered = CompletableDeferred<ObservationReducers.ReducerMessage>()
        every { stub.observe(any(), any()) } answers {
            val requests = firstArg<Flow<ObservationReducers.ReducerMessage>>()
            if (observeCalls.incrementAndGet() == 1) {
                flow { throw clientIsNotConnected.asRuntimeException() }
            } else {
                flow {
                    launch { reRegistered.complete(requests.first()) }
                    awaitCancellation()
                }
            }
        }
        val recorded = RecordedWaits(1)
        val reconnectRequest = async(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) { lifecycle.reconnectRequests.first() }

        val job = reducersService(lifecycle, stub, recorded).register(ReconnectingReducer())
        try {
            assertEquals(stale, withTimeout(5_000) { reconnectRequest.await() })

            lifecycle.markDisconnected()
            lifecycle.markConnected(lifecycle.connectionId)
            val registration = withTimeout(5_000) { reRegistered.await() }.content.value0

            assertNotEquals(stale, registration.connectionId)
            assertEquals(lifecycle.connectionId, registration.connectionId)
            assertEquals(2, observeCalls.get())
            assertTrue(recorded.waits.isEmpty(), "the stale stream must not be retried, but it waited ${recorded.waits}")
        } finally {
            job.cancel()
        }
    }

    @Test
    fun `an unavailable kernel is not a lost session`() {
        assertFalse(Status.UNAVAILABLE.withDescription("io exception").asException().isSessionLost())
    }

    @Test
    fun `a client the kernel does not know is a lost session, however it is wrapped`() {
        assertTrue(clientIsNotConnected.asException().isSessionLost())
        assertTrue(clientIsNotConnected.asRuntimeException().isSessionLost())
        assertTrue(RuntimeException("wrapped", clientIsNotConnected.asRuntimeException()).isSessionLost())
    }
}
