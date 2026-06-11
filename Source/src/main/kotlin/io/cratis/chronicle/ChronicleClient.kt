// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import io.cratis.chronicle.connection.ChronicleConnection
import java.util.concurrent.ConcurrentHashMap

class ChronicleClient(private val options: ChronicleOptions) : IChronicleClient {
    private val connection = ChronicleConnection(options.connectionString).also { it.connect() }
    private val eventStores = ConcurrentHashMap<String, EventStore>()

    override fun getEventStore(name: String, namespace: String): EventStore {
        return eventStores.getOrPut("$name/$namespace") {
            EventStore(name, namespace, connection.services, connection.connectionId, options.defaultSinkTypeId)
        }
    }

    override fun dispose() {
        connection.disconnect()
    }
}
