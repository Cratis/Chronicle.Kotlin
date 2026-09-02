// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import Cratis.Chronicle.Contracts.EventStores.EventStoresGrpcKt
import Cratis.Chronicle.Contracts.EventStores.Eventstores
import com.google.protobuf.Empty
import io.cratis.chronicle.connection.ChronicleConnectionString
import io.grpc.Grpc
import io.grpc.InsecureServerCredentials
import io.grpc.Server
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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
            override fun allEventStores(request: Empty): Flow<Eventstores.QueryResult_IEnumerable_String> =
                flowOf(Eventstores.QueryResult_IEnumerable_String.newBuilder().addAllData(names).build())
        }
        return Grpc.newServerBuilderForPort(0, InsecureServerCredentials.create())
            .addService(impl)
            .build()
            .start()
            .also { server = it }
    }

    private fun clientFor(port: Int): ChronicleClient {
        val connectionString = ChronicleConnectionString.parse("chronicle://localhost:$port?disableTls=true&apiKey=test")
        return ChronicleClient(ChronicleOptions(connectionString)).also { client = it }
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
