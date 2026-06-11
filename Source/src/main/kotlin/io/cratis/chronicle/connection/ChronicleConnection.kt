// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.TimeUnit

/**
 * Manages the gRPC channel lifecycle and exposes Chronicle service stubs.
 */
class ChronicleConnection(private val connectionString: ChronicleConnectionString) : AutoCloseable {

    private val tokenProvider: ITokenProvider = createTokenProvider()
    private val channel: ManagedChannel by lazy { createChannel() }

    val services: ChronicleServices by lazy { ChronicleServices(channel) }

    private val connectionManager: ConnectionManager by lazy {
        ConnectionManager(services.connection).also { it.connect() }
    }

    /** The stable client connection ID shared across all reducer and reactor registrations. */
    val connectionId: String get() = connectionManager.connectionId

    private fun createTokenProvider(): ITokenProvider {
        val hasApiKey = connectionString.apiKey != null
        if (hasApiKey) return NoOpTokenProvider

        val username = connectionString.username
            ?: ChronicleConnectionString.DEVELOPMENT_CLIENT
        val password = connectionString.password
            ?: ChronicleConnectionString.DEVELOPMENT_CLIENT_SECRET

        val scheme = if (connectionString.disableTls) "http" else "https"
        val tokenEndpoint =
            "$scheme://${connectionString.host}:${connectionString.managementPort}/connect/token"

        return OAuthTokenProvider(tokenEndpoint, username, password)
    }

    private fun createChannel(): ManagedChannel {
        val builder = if (connectionString.disableTls) {
            ManagedChannelBuilder.forAddress(connectionString.host, connectionString.port)
                .usePlaintext()
        } else {
            ManagedChannelBuilder.forAddress(connectionString.host, connectionString.port)
        }
        builder.intercept(BearerTokenInterceptor(tokenProvider))
        return builder.build()
    }

    fun connect() {
        @Suppress("UNUSED_EXPRESSION")
        connectionManager
    }

    fun disconnect() {
        connectionManager.disconnect()
        if (!channel.isShutdown) {
            channel.shutdown()
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow()
            }
        }
    }

    override fun close() = disconnect()
}
