// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import io.cratis.chronicle.diagnostics.ChronicleTraces
import com.google.protobuf.Empty
import kotlinx.coroutines.flow.first
import io.cratis.chronicle.connection.ChronicleConnection
import java.util.concurrent.ConcurrentHashMap

class ChronicleClient(private val options: ChronicleOptions) : IChronicleClient {
    private val connection = ChronicleConnection(options.connectionString).also { it.connect() }

    // Built once per client so every event store shares one tracer. Resolving the global
    // OpenTelemetry is deferred to first use, so a client constructed before the SDK is installed
    // still ends up reporting to the real one.
    private val traces = ChronicleTraces(options.openTelemetry)
    private val eventStores = ConcurrentHashMap<String, EventStore>()

    override fun getEventStore(name: String, namespace: String): EventStore {
        return eventStores.computeIfAbsent("$name/$namespace") {
            EventStore(
                name,
                namespace,
                connection.services,
                connection.lifecycle,
                options.defaultSinkTypeId,
                options.artifacts,
                options.artifactActivator,
                options.autoDiscoverAndRegister,
                traces
            )
        }
    }

    override suspend fun getEventStores(): List<String> {
        val request = Empty.getDefaultInstance()
        // An observable query on the kernel side: it streams the whole list again whenever it
        // changes. This asks the question once, so it takes the first answer and unsubscribes.
        return connection.services.eventStores.allEventStores(request).first().dataList
    }

    override fun evictEventStores() {
        eventStores.clear()
    }

    override fun dispose() {
        connection.disconnect()
    }
}
