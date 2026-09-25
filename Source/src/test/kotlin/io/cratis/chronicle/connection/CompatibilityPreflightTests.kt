// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import Cratis.Chronicle.Contracts.Clients.Clients
import Cratis.Chronicle.Contracts.Clients.ConnectionServiceGrpcKt
import Cratis.Chronicle.Contracts.Sequences.EventSequencesGrpcKt
import Cratis.Chronicle.Contracts.Sequences.Sequences
import io.grpc.Grpc
import io.grpc.InsecureServerCredentials
import io.grpc.Server
import io.grpc.Status
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class CompatibilityPreflightTests {
    private lateinit var server: Server
    private lateinit var connection: ChronicleConnection
    private val checks = AtomicInteger()
    private val writes = AtomicInteger()
    @Volatile private var compatible = true
    @Volatile private var unavailable = false
    @Volatile private var contradictory = false

    @BeforeEach
    fun establish() {
        server = Grpc.newServerBuilderForPort(0, InsecureServerCredentials.create())
            .addService(object : ConnectionServiceGrpcKt.ConnectionServiceCoroutineImplBase() {
                override suspend fun checkCompatibility(request: Clients.CompatibilityRequest): Clients.CompatibilityResponse {
                    checks.incrementAndGet()
                    assertFalse(request.descriptorSet.isEmpty)
                    if (unavailable) throw Status.UNAVAILABLE.asRuntimeException()
                    return Clients.CompatibilityResponse.newBuilder().setIsCompatible(compatible)
                        .apply { if (!compatible || contradictory) addIncompatibilities("unsupported contract") }.build()
                }
            })
            .addService(object : EventSequencesGrpcKt.EventSequencesCoroutineImplBase() {
                override suspend fun append(request: Sequences.AppendRequest): Sequences.CommandResult_AppendResponse {
                    writes.incrementAndGet()
                    return Sequences.CommandResult_AppendResponse.newBuilder().setIsAuthorized(true).build()
                }
            }).build().start()
        connection = createConnection()
    }

    private fun createConnection() = ChronicleConnection(ChronicleConnectionString.parse(
        "chronicle://127.0.0.1:${server.port}?disableTls=true"))

    @AfterEach
    fun destroy() {
        connection.close()
        server.shutdownNow()
    }

    @Test
    fun `direct writes cannot bypass an incompatible channel`() = runBlocking {
        compatible = false
        assertThrows(IllegalStateException::class.java) { connection.services }
        assertEquals(0, writes.get())
        assertEquals(1, checks.get())
    }

    @Test
    fun `concurrent first users share one successful channel check`() = runBlocking {
        (1..8).map { async(Dispatchers.IO) {
            connection.services.eventSequences.append(Sequences.AppendRequest.getDefaultInstance())
        } }.awaitAll()
        assertEquals(1, checks.get())
        assertEquals(8, writes.get())
    }

    @Test
    fun `unavailable verification remains retryable without writing first`() = runBlocking {
        unavailable = true
        assertThrows(Exception::class.java) { connection.services }
        assertEquals(0, writes.get())
        unavailable = false
        connection.services.eventSequences.append(Sequences.AppendRequest.getDefaultInstance())
        assertEquals(2, checks.get())
        assertEquals(1, writes.get())
    }

    @Test
    fun `a contradictory verdict cannot permit a write`() {
        contradictory = true
        assertThrows(IllegalStateException::class.java) { connection.services }
        assertEquals(0, writes.get())
    }

    @Test
    fun `a new channel must obtain its own verdict`() = runBlocking {
        connection.services.eventSequences.append(Sequences.AppendRequest.getDefaultInstance())
        connection.close()
        compatible = false
        connection = createConnection()
        assertThrows(IllegalStateException::class.java) { connection.services }
        assertEquals(2, checks.get())
        assertEquals(1, writes.get())
    }
}
