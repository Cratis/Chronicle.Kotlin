// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import Cratis.Chronicle.Contracts.Clients.Clients
import Cratis.Chronicle.Contracts.Clients.ConnectionServiceGrpcKt
import kotlinx.coroutines.flow.Flow

/**
 * The connection service operations [ConnectionManager] needs, so the keep-alive can be
 * exercised without a live kernel.
 */
interface IKeepAliveConnections {
    /** Opens the keep-alive stream the kernel pushes to. */
    fun connect(request: Clients.ConnectRequest): Flow<Clients.ConnectionKeepAlive>

    /** Answers a keep-alive, which is what bumps `LastSeen` on the kernel. */
    suspend fun answer(connectionId: String)
}

/** Adapts the generated gRPC stub to [IKeepAliveConnections]. */
class KeepAliveConnections(
    private val stub: ConnectionServiceGrpcKt.ConnectionServiceCoroutineStub
) : IKeepAliveConnections {

    override fun connect(request: Clients.ConnectRequest): Flow<Clients.ConnectionKeepAlive> =
        stub.connect(request)

    override suspend fun answer(connectionId: String) {
        stub.connectionKeepAlive(
            Clients.ConnectionKeepAlive.newBuilder()
                .setConnectionId(connectionId)
                .build()
        )
    }
}
