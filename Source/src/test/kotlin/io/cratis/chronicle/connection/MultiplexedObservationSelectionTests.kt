// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import Cratis.Chronicle.Contracts.Clients.Clients
import Cratis.Chronicle.Contracts.Clients.ConnectionServiceGrpcKt
import Cratis.Chronicle.Contracts.Observation.Multiplexed.MultiplexedObservationGrpcKt
import Cratis.Chronicle.Contracts.Observation.Multiplexed.ObservationMultiplexed
import Cratis.Chronicle.Contracts.Observation.Reactors.ObservationReactors
import Cratis.Chronicle.Contracts.Observation.Reactors.ReactorsGrpcKt
import Cratis.Chronicle.Contracts.Observation.Reducers.ObservationReducers
import Cratis.Chronicle.Contracts.Observation.Reducers.ReducersGrpcKt
import io.grpc.Grpc
import io.grpc.InsecureServerCredentials
import io.grpc.Server
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

/** Whether a reactor or reducer observes over the shared call or its own, decided by what the kernel advertises. */
class MultiplexedObservationSelectionTests {
    private lateinit var server: Server
    private var connection: ChronicleConnection? = null
    private val perObserverReactorCalls = AtomicInteger()
    private val perObserverReducerCalls = AtomicInteger()
    private val sharedCalls = AtomicInteger()

    @Volatile private var capabilities: List<String> = listOf("multiplexed-observation")

    @BeforeEach
    fun establish() {
        server = Grpc.newServerBuilderForPort(0, InsecureServerCredentials.create())
            .addService(object : ConnectionServiceGrpcKt.ConnectionServiceCoroutineImplBase() {
                override suspend fun checkCompatibility(request: Clients.CompatibilityRequest): Clients.CompatibilityResponse =
                    Clients.CompatibilityResponse.newBuilder().setIsCompatible(true).addAllCapabilities(capabilities).build()
            })
            .addService(object : ReactorsGrpcKt.ReactorsCoroutineImplBase() {
                override fun observe(requests: Flow<ObservationReactors.ReactorMessage>): Flow<ObservationReactors.EventsToObserve> = flow {
                    perObserverReactorCalls.incrementAndGet()
                    emit(ObservationReactors.EventsToObserve.newBuilder().setPartition("own call").build())
                    awaitCancellation()
                }
            })
            .addService(object : ReducersGrpcKt.ReducersCoroutineImplBase() {
                override fun observe(requests: Flow<ObservationReducers.ReducerMessage>): Flow<ObservationReducers.ReduceOperationMessage> = flow {
                    perObserverReducerCalls.incrementAndGet()
                    emit(ObservationReducers.ReduceOperationMessage.newBuilder().setPartition("own call").build())
                    awaitCancellation()
                }
            })
            .addService(object : MultiplexedObservationGrpcKt.MultiplexedObservationCoroutineImplBase() {
                override fun observe(requests: Flow<ObservationMultiplexed.MultiplexedClientMessage>): Flow<ObservationMultiplexed.MultiplexedServerMessage> = channelFlow {
                    sharedCalls.incrementAndGet()
                    requests.collect { write ->
                        for (message in write.messagesList.filter { !it.close }) {
                            val answer = ObservationMultiplexed.MultiplexedServerMessage.newBuilder().setStreamId(message.streamId).setSequence(1)
                            if (message.kind == ObservationMultiplexed.MultiplexedObserverKind.Reactor) {
                                answer.setReactor(ObservationMultiplexed.EventsToObserve.newBuilder().setPartition("shared call"))
                            } else {
                                answer.setReducer(ObservationMultiplexed.ReduceOperationMessage.newBuilder().setPartition("shared call"))
                            }
                            send(answer.build())
                        }
                    }
                }
            })
            .build().start()
    }

    @AfterEach
    fun destroy() {
        connection?.close()
        server.shutdownNow()
    }

    private fun connect(query: String = "", configure: ChronicleConnection.() -> Unit = {}): ChronicleConnection =
        ChronicleConnection(ChronicleConnectionString.parse("chronicle://127.0.0.1:${server.port}?disableTls=true$query"))
            .also { it.configure() }
            .also { connection = it }

    private fun registration(): Flow<ObservationReactors.ReactorMessage> = flow {
        emit(ObservationReactors.ReactorMessage.getDefaultInstance())
        awaitCancellation()
    }

    private fun reducerRegistration(): Flow<ObservationReducers.ReducerMessage> = flow {
        emit(ObservationReducers.ReducerMessage.getDefaultInstance())
        awaitCancellation()
    }

    private fun observedReactorPartition(connection: ChronicleConnection): String = runBlocking {
        withTimeout(10_000) { connection.services.observeReactor(registration()).first().partition }
    }

    private fun observedReducerPartition(connection: ChronicleConnection): String = runBlocking {
        withTimeout(10_000) { connection.services.observeReducer(reducerRegistration()).first().partition }
    }

    @Test
    fun `a reactor observes over the shared call when the kernel advertises it`() {
        assertEquals("shared call", observedReactorPartition(connect()))
        assertEquals(1, sharedCalls.get())
        assertEquals(0, perObserverReactorCalls.get())
    }

    @Test
    fun `a reducer observes over the shared call when the kernel advertises it`() {
        assertEquals("shared call", observedReducerPartition(connect()))
        assertEquals(1, sharedCalls.get())
        assertEquals(0, perObserverReducerCalls.get())
    }

    @Test
    fun `a reactor and a reducer share one call`() {
        val connected = connect()
        observedReactorPartition(connected)
        observedReducerPartition(connected)
        // The first observer's call is closed once it is done; what counts is that neither used a call of its own.
        assertEquals(0, perObserverReactorCalls.get() + perObserverReducerCalls.get())
    }

    @Test
    fun `a reactor observes on a call of its own when the kernel does not advertise sharing`() {
        capabilities = emptyList()
        assertEquals("own call", observedReactorPartition(connect()))
        assertEquals(1, perObserverReactorCalls.get())
        assertEquals(0, sharedCalls.get())
    }

    @Test
    fun `a reducer observes on a call of its own when the kernel does not advertise sharing`() {
        capabilities = emptyList()
        assertEquals("own call", observedReducerPartition(connect()))
        assertEquals(1, perObserverReducerCalls.get())
        assertEquals(0, sharedCalls.get())
    }

    @Test
    fun `a reactor observes on a call of its own when sharing is turned off`() {
        assertEquals("own call", observedReactorPartition(connect { multiplexObservation = false }))
        assertEquals(1, perObserverReactorCalls.get())
        assertEquals(0, sharedCalls.get())
    }

    @Test
    fun `a reactor observes on a call of its own when the compatibility check was skipped`() {
        assertEquals("own call", observedReactorPartition(connect("&skipCompatibilityCheck=true")))
        assertEquals(1, perObserverReactorCalls.get())
        assertEquals(0, sharedCalls.get())
    }
}
