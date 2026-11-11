// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import Cratis.Chronicle.Contracts.Observation.Reactors.ReactorsGrpcKt
import Cratis.Chronicle.Contracts.Observation.Reducers.ObservationReducers
import Cratis.Chronicle.Contracts.Observation.Reducers.ReducersGrpcKt
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.diagnostics.ChronicleTraces
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.sinks.WellKnownSinkTypes
import io.grpc.Status
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
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

    @Reactor("throwing-reactor")
    class ThrowingReactor(private val error: Exception) {
        @Observes(AccountOpened::class)
        fun opened(@Suppress("UNUSED_PARAMETER") batch: EventBatch): Unit = throw error
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

    private fun reactorsService(
        lifecycle: ConnectionLifecycle,
        stub: ReactorsGrpcKt.ReactorsCoroutineStub,
        wait: suspend (Duration) -> Unit
    ) = ReactorsService(
        "store", "default", lifecycle, stub, mockk<IEventLog>(),
        ReactorMiddlewares.none, ReactorMethodArguments.contextOnly, ChronicleTraces.default,
        ::fixedBackoff, wait
    )

    private fun reducersService(
        lifecycle: ConnectionLifecycle,
        stub: ReducersGrpcKt.ReducersCoroutineStub,
        wait: suspend (Duration) -> Unit
    ) = ReducersService(
        "store", "default", lifecycle, stub,
        WellKnownSinkTypes.MONGODB, null, ChronicleTraces.default,
        ::fixedBackoff, wait
    )

    @Test
    fun `a reactor whose streams keep ending without working waits longer each time`() = runBlocking {
        val lifecycle = ConnectionLifecycle().apply { markConnected(connectionId) }
        val stub = mockk<ReactorsGrpcKt.ReactorsCoroutineStub>()
        every { stub.observe(any(), any()) } answers { flow { } }
        val recorded = RecordedWaits(4)

        val job = reactorsService(lifecycle, stub, recorded.wait).register(ReconnectingReactor())
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

        val job = reactorsService(lifecycle, stub, recorded.wait).register(ReconnectingReactor())
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

        val job = reducersService(lifecycle, stub, recorded.wait).register(ReconnectingReducer())
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

        val job = reducersService(lifecycle, stub, recorded.wait).register(ReconnectingReducer())
        try {
            withTimeout(5_000) { recorded.done.await() }
        } finally {
            job.cancel()
        }

        assertEquals(List(4) { 2.seconds }, recorded.waits)
    }

    @Test
    fun `a new connection cancels a pending wait, opens at once, and keeps the backoff it had grown`() = runBlocking {
        val lifecycle = ConnectionLifecycle().apply { markConnected(connectionId) }
        val first = lifecycle.connectionId
        val stub = mockk<ReactorsGrpcKt.ReactorsCoroutineStub>()
        val steps = CopyOnWriteArrayList<String>()
        every { stub.observe(any(), any()) } answers {
            val requests = firstArg<Flow<ObservationReactors.ReactorMessage>>()
            // The registration is sent before the stream opens, so it is already buffered.
            flow { steps += "observe ${requests.first().content.value0.connectionId}" }
        }
        val waits = AtomicInteger()
        val blockedInSecondWait = CompletableDeferred<Unit>()
        val blockedInThirdWait = CompletableDeferred<Unit>()
        val wait: suspend (Duration) -> Unit = { delay ->
            steps += "wait $delay"
            when (waits.incrementAndGet()) {
                2 -> { blockedInSecondWait.complete(Unit); awaitCancellation() }
                3 -> { blockedInThirdWait.complete(Unit); awaitCancellation() }
            }
        }

        val job = reactorsService(lifecycle, stub, wait).register(ReconnectingReactor())
        try {
            withTimeout(5_000) { blockedInSecondWait.await() }
            lifecycle.markDisconnected()
            lifecycle.markConnected(lifecycle.connectionId)
            withTimeout(5_000) { blockedInThirdWait.await() }
        } finally {
            job.cancel()
        }

        val second = lifecycle.connectionId
        assertNotEquals(first, second)
        assertEquals(
            listOf("observe $first", "wait 2s", "observe $first", "wait 4s", "observe $second", "wait 8s"),
            steps.toList()
        )
    }

    @Test
    fun `a stream error that is not a lost session backs off without reconnecting the client`() = runBlocking {
        val lifecycle = ConnectionLifecycle().apply { markConnected(connectionId) }
        val stub = mockk<ReactorsGrpcKt.ReactorsCoroutineStub>()
        every { stub.observe(any(), any()) } answers {
            flow { throw Status.UNAVAILABLE.withDescription("ClientIsNotConnected").asException() }
        }
        val recorded = RecordedWaits(2)
        val reconnectRequest = async(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) { lifecycle.reconnectRequests.first() }

        val job = reactorsService(lifecycle, stub, recorded.wait).register(ReconnectingReactor())
        try {
            withTimeout(5_000) { recorded.done.await() }
            assertNull(withTimeoutOrNull(200) { reconnectRequest.await() })
        } finally {
            reconnectRequest.cancel()
            job.cancel()
        }

        assertEquals(listOf(2, 4).map { it.seconds }, recorded.waits)
    }

    @Test
    fun `a handler failing with a lost session error fails its partition and never reconnects the client`() = runBlocking {
        val lifecycle = ConnectionLifecycle().apply { markConnected(connectionId) }
        val stub = mockk<ReactorsGrpcKt.ReactorsCoroutineStub>()
        val answered = CompletableDeferred<ObservationReactors.ReactorMessage>()
        val event = ObservationReactors.AppendedEvent.newBuilder()
            .setContext(
                ObservationReactors.EventContext.newBuilder()
                    .setSequenceNumber(1)
                    .setEventSourceId("a")
                    .setEventType(ObservationReactors.EventType.newBuilder().setId("AccountOpened").setGeneration(1))
            )
            .setContent("{\"owner\":\"a\"}")
            .build()
        val page = ObservationReactors.EventsToObserve.newBuilder()
            .setDeliveryId(1)
            .addPartitions(ObservationReactors.PartitionEvents.newBuilder().setPartition("a").addEvents(event))
            .build()
        every { stub.observe(any(), any()) } answers {
            val requests = firstArg<Flow<ObservationReactors.ReactorMessage>>()
            flow {
                coroutineScope {
                    launch {
                        var registered = false
                        requests.collect { if (registered) answered.complete(it) else registered = true }
                    }
                    emit(page)
                    awaitCancellation()
                }
            }
        }
        val recorded = RecordedWaits(1)
        val reconnectRequest = async(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) { lifecycle.reconnectRequests.first() }

        val job = reactorsService(lifecycle, stub, recorded.wait)
            .register(ThrowingReactor(clientIsNotConnected.asRuntimeException()))
        try {
            val result = withTimeout(5_000) { answered.await() }.content.value1
            assertEquals(listOf(ObservationReactors.ObservationState.Failed), result.partitionsList.map { it.state })
            assertNull(withTimeoutOrNull(200) { reconnectRequest.await() })
            assertTrue(recorded.waits.isEmpty(), "the stream must stay open, but it waited ${recorded.waits}")
        } finally {
            reconnectRequest.cancel()
            job.cancel()
        }
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
                    coroutineScope {
                        launch { requests.firstOrNull()?.let(reRegistered::complete) }
                        awaitCancellation()
                    }
                }
            }
        }
        val recorded = RecordedWaits(1)
        val reconnectRequest = async(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) { lifecycle.reconnectRequests.first() }

        val job = reactorsService(lifecycle, stub, recorded.wait).register(ReconnectingReactor())
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
                    coroutineScope {
                        launch { requests.firstOrNull()?.let(reRegistered::complete) }
                        awaitCancellation()
                    }
                }
            }
        }
        val recorded = RecordedWaits(1)
        val reconnectRequest = async(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) { lifecycle.reconnectRequests.first() }

        val job = reducersService(lifecycle, stub, recorded.wait).register(ReconnectingReducer())
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
    fun `an unavailable status naming the lost connection is still a transport condition`() {
        assertFalse(Status.UNAVAILABLE.withDescription("ClientIsNotConnected").asException().isSessionLost())
        assertFalse(Status.UNAVAILABLE.withDescription("ClientIsNotConnected").asRuntimeException().isSessionLost())
    }

    @Test
    fun `a cancelled status naming the lost connection is still a transport condition`() {
        assertFalse(Status.CANCELLED.withDescription("ClientIsNotConnected").asException().isSessionLost())
        assertFalse(Status.CANCELLED.withDescription("ClientIsNotConnected").asRuntimeException().isSessionLost())
    }

    @Test
    fun `only the first gRPC status in the cause chain decides`() {
        val outer = Status.INTERNAL.withDescription("something else").withCause(clientIsNotConnected.asRuntimeException())
        assertFalse(RuntimeException("wrapped", outer.asRuntimeException()).isSessionLost())
    }

    @Test
    fun `an application error that is not a gRPC status is not a lost session`() {
        assertFalse(IllegalStateException("ClientIsNotConnected").isSessionLost())
    }

    @Test
    fun `a client the kernel does not know is a lost session, however it is wrapped`() {
        assertTrue(clientIsNotConnected.asException().isSessionLost())
        assertTrue(clientIsNotConnected.asRuntimeException().isSessionLost())
        assertTrue(RuntimeException("wrapped", clientIsNotConnected.asRuntimeException()).isSessionLost())
    }
}
