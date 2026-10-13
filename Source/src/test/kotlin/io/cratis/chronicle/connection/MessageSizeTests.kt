// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import Cratis.Chronicle.Contracts.Clients.Clients
import Cratis.Chronicle.Contracts.Clients.ConnectionServiceGrpcKt
import Cratis.Chronicle.Contracts.Sequences.EventSequencesGrpcKt
import Cratis.Chronicle.Contracts.Sequences.Sequences
import io.cratis.chronicle.ChronicleOptions
import io.grpc.Grpc
import io.grpc.InsecureServerCredentials
import io.grpc.Server
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * grpc-java accepts 4 MB unless told otherwise, so a response past that - a read model that has grown, a large
 * event - would be refused by a client that never configured anything.
 */
class MessageSizeTests {
    private lateinit var server: Server
    private val fiveMegabytes = "x".repeat(5 * 1024 * 1024)

    @BeforeEach
    fun establish() {
        server = Grpc.newServerBuilderForPort(0, InsecureServerCredentials.create())
            .addService(object : ConnectionServiceGrpcKt.ConnectionServiceCoroutineImplBase() {
                override suspend fun checkCompatibility(request: Clients.CompatibilityRequest): Clients.CompatibilityResponse =
                    Clients.CompatibilityResponse.newBuilder().setIsCompatible(true).build()
            })
            .addService(object : EventSequencesGrpcKt.EventSequencesCoroutineImplBase() {
                override suspend fun append(request: Sequences.AppendRequest): Sequences.CommandResult_AppendResponse =
                    Sequences.CommandResult_AppendResponse.newBuilder().setIsAuthorized(true).addExceptionMessages(fiveMegabytes).build()
            }).build().start()
    }

    @AfterEach
    fun destroy() {
        server.shutdownNow()
    }

    private fun connect(maxReceiveMessageSize: Int?) = ChronicleConnection(
        ChronicleConnectionString.parse("chronicle://127.0.0.1:${server.port}?disableTls=true"),
        maxReceiveMessageSize
    )

    @Test
    fun `default options accept a message larger than the grpc default`() = runBlocking {
        connect(ChronicleOptions.development().maxReceiveMessageSize).use {
            val response = it.services.eventSequences.append(Sequences.AppendRequest.getDefaultInstance())
            assertEquals(fiveMegabytes, response.getExceptionMessages(0))
        }
    }

    @Test
    fun `the connection defaults to 100 MB when nothing is passed`() = runBlocking {
        ChronicleConnection(ChronicleConnectionString.parse("chronicle://127.0.0.1:${server.port}?disableTls=true")).use {
            assertEquals(1, it.services.eventSequences.append(Sequences.AppendRequest.getDefaultInstance()).exceptionMessagesCount)
        }
    }

    @Test
    fun `a configured size is enforced`() {
        connect(1024 * 1024).use {
            val failure = assertThrows(StatusException::class.java) {
                runBlocking { it.services.eventSequences.append(Sequences.AppendRequest.getDefaultInstance()) }
            }
            assertEquals(Status.Code.RESOURCE_EXHAUSTED, failure.status.code)
        }
    }

    @Test
    fun `null keeps the grpc default`() {
        connect(null).use {
            val failure = assertThrows(StatusException::class.java) {
                runBlocking { it.services.eventSequences.append(Sequences.AppendRequest.getDefaultInstance()) }
            }
            assertEquals(Status.Code.RESOURCE_EXHAUSTED, failure.status.code)
        }
    }
}
