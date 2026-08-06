// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import com.google.protobuf.Empty
import io.cratis.chronicle.connection.ChronicleConnection
import java.util.concurrent.ConcurrentHashMap

class ChronicleClient(private val options: ChronicleOptions) : IChronicleClient {
    private val connection = ChronicleConnection(options.connectionString).also { it.connect() }
    private val eventStores = ConcurrentHashMap<String, EventStore>()

    override fun getEventStore(name: String, namespace: String): EventStore {
        return eventStores.getOrPut("$name/$namespace") {
            EventStore(
                name,
                namespace,
                connection.services,
                connection.lifecycle,
                options.defaultSinkTypeId,
                options.artifacts,
                options.artifactActivator,
                options.autoDiscoverAndRegister
            )
        }
    }

    override suspend fun getEventStores(): List<String> {
        val request = Empty.getDefaultInstance()
        return connection.services.eventStores.getEventStores(request).itemsList
    }

    override fun evictEventStores() {
        eventStores.clear()
    }

    override fun dispose() {
        connection.disconnect()
    }
}
