// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import Cratis.Chronicle.Contracts.Clients.Clients
import Cratis.Chronicle.Contracts.Clients.ConnectionServiceGrpcKt
import Cratis.Chronicle.Contracts.EventStores.EventStoresGrpcKt
import Cratis.Chronicle.Contracts.EventStores.Eventstores
import com.google.protobuf.Empty
import io.cratis.chronicle.connection.ChronicleConnectionString
import io.grpc.Grpc
import io.grpc.InsecureServerCredentials
import io.grpc.Server
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class ChronicleClientTests {

    private var server: Server? = null
    private var client: ChronicleClient? = null

    @AfterEach
    fun tearDown() {
        client?.dispose()
        server?.shutdownNow()
    }

    private fun startServerWithEventStores(names: List<String>): Server {
        val impl = object : EventStoresGrpcKt.EventStoresCoroutineImplBase() {
            override suspend fun allEventStores(request: Empty): Eventstores.QueryResult_IEnumerable_EventStoreNamesResponse =
                Eventstores.QueryResult_IEnumerable_EventStoreNamesResponse.newBuilder()
                    .addAllData(names.map { Eventstores.EventStoreNamesResponse.newBuilder().setName(it).build() })
                    .setIsAuthorized(true)
                    .build()
        }
        return Grpc.newServerBuilderForPort(0, InsecureServerCredentials.create())
            .addService(impl)
            .addService(object : ConnectionServiceGrpcKt.ConnectionServiceCoroutineImplBase() {
                override suspend fun checkCompatibility(request: Clients.CompatibilityRequest): Clients.CompatibilityResponse =
                    Clients.CompatibilityResponse.newBuilder().setIsCompatible(true).build()
            })
            .build()
            .start()
            .also { server = it }
    }

    private fun clientFor(port: Int): ChronicleClient {
        val connectionString = ChronicleConnectionString.parse("chronicle://localhost:$port?disableTls=true")
        return ChronicleClient(ChronicleOptions(connectionString)).also { client = it }
    }

    @Test
    fun `rejects an API key at client construction instead of silently omitting credentials`() {
        val options = ChronicleOptions(ChronicleConnectionString.parse("chronicle://localhost:1?apiKey=test"))

        val error = assertThrows(IllegalArgumentException::class.java) { ChronicleClient(options) }

        assertTrue(error.message!!.contains("API key authentication is not supported by the Chronicle kernel"))
        assertTrue(error.message!!.contains("client id and secret"))
    }

    @Test
    fun `getEventStores lists every event store known to the kernel, not just cached ones`() = runBlocking {
        val server = startServerWithEventStores(listOf("store-one", "store-two"))
        val client = clientFor(server.port)

        val stores = client.getEventStores()

        assertEquals(listOf("store-one", "store-two"), stores)
    }

    @Test
    fun `evictEventStores clears the cache so a subsequent getEventStore returns a fresh instance`() {
        val server = startServerWithEventStores(emptyList())
        val client = clientFor(server.port)

        val first = client.getEventStore("my-store")
        val second = client.getEventStore("my-store")
        assertSame(first, second, "getEventStore should return the cached instance before eviction")

        client.evictEventStores()

        val third = client.getEventStore("my-store")
        assertNotSame(first, third, "getEventStore should return a fresh instance after eviction")
    }
}
