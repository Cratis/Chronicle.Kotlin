// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import Cratis.Chronicle.Contracts.Clients.Clients
import Cratis.Chronicle.Contracts.Clients.ConnectionServiceGrpcKt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Manages the Chronicle client connection lifecycle.
 *
 * Establishes a [Connect][ConnectionServiceGrpcKt.ConnectionServiceCoroutineStub.connect] stream
 * with the Chronicle server and responds to keep-alive pings for the duration of the process.
 */
class ConnectionManager(
    private val stub: ConnectionServiceGrpcKt.ConnectionServiceCoroutineStub
) {
    /** Stable client identity shared across all reducer and reactor registrations. */
    val connectionId: String = UUID.randomUUID().toString()

    private var keepAliveJob: Job? = null

    /** Establishes the server connection and starts the keep-alive loop in the background. */
    fun connect() {
        keepAliveJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Clients.ConnectRequest.newBuilder()
                    .setConnectionId(connectionId)
                    .setClientVersion("1.0.0")
                    .setIsRunningWithDebugger(false)
                    .build()

                stub.connect(request).collect { keepAlive ->
                    try {
                        stub.connectionKeepAlive(
                            Clients.ConnectionKeepAlive.newBuilder()
                                .setConnectionId(keepAlive.connectionId)
                                .build()
                        )
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    /** Cancels the keep-alive background job. */
    fun disconnect() {
        keepAliveJob?.cancel()
        keepAliveJob = null
    }
}
