// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import io.grpc.Grpc
import io.grpc.ManagedChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

/**
 * Manages the gRPC channel lifecycle and exposes Chronicle service stubs.
 */
class ChronicleConnection(private val connectionString: ChronicleConnectionString) : AutoCloseable {

    private val srvResolver = SrvResolver()
    private val loadBalancerStrategy = LoadBalancerStrategy.forConnectionString(connectionString)

    /**
     * The server address the channel and token provider connect to.
     *
     * Resolved once per [ChronicleConnection] instance: a `chronicle+srv://` connection string is
     * looked up via DNS through [srvResolver], then [loadBalancerStrategy] picks one address from
     * the result (or from the connection string's explicit [ChronicleConnectionString.addresses]
     * for a non-SRV, possibly multi-host, connection string). Any reconnect/retry logic added to
     * this class in the future should call [resolveAndSelect] again rather than reuse this cached
     * value, so a server that comes up, goes down, or a DNS record that changes is picked up on
     * the next attempt.
     */
    private val selectedAddress: ChronicleServerAddress by lazy {
        runBlocking(Dispatchers.IO) { resolveAndSelect() }
    }

    private val tokenProvider: ITokenProvider by lazy { createTokenProvider() }
    private val channel: ManagedChannel by lazy { createChannel() }

    val services: ChronicleServices by lazy { ChronicleServices(channel) }

    private val connectionManager: ConnectionManager by lazy {
        ConnectionManager(KeepAliveConnections(services.connection)).also { it.connect() }
    }

    /** Tracks whether the client is connected, and under which connection ID. */
    val lifecycle: ConnectionLifecycle get() = connectionManager.lifecycle

    /** The current client connection ID. Rotates on every reconnect. */
    val connectionId: String get() = connectionManager.connectionId

    private suspend fun resolveAndSelect(): ChronicleServerAddress {
        val addresses = if (connectionString.isSrv) {
            srvResolver.resolve(connectionString.host, connectionString.srvNameServer)
        } else {
            connectionString.addresses
        }
        return loadBalancerStrategy.select(addresses)
    }

    private fun createTokenProvider(): ITokenProvider {
        val hasApiKey = connectionString.apiKey != null
        if (hasApiKey) return NoOpTokenProvider

        val username = connectionString.username
            ?: ChronicleConnectionString.DEVELOPMENT_CLIENT
        val password = connectionString.password
            ?: ChronicleConnectionString.DEVELOPMENT_CLIENT_SECRET

        val scheme = if (connectionString.disableTls) "http" else "https"
        val tokenEndpoint =
            "$scheme://${selectedAddress.host}:${selectedAddress.port}/connect/token"

        return OAuthTokenProvider(
            tokenEndpoint,
            username,
            password,
            connectionString.disableTls,
            connectionString.skipTlsValidation
        )
    }

    private fun createChannel(): ManagedChannel {
        val builder = Grpc.newChannelBuilderForAddress(
            selectedAddress.host,
            selectedAddress.port,
            connectionString.createCredentials()
        )
        builder.intercept(BearerTokenInterceptor(tokenProvider))
        return builder.build()
    }

    fun connect() {
        @Suppress("UNUSED_EXPRESSION")
        connectionManager
    }

    fun disconnect() {
        connectionManager.close()
        if (!channel.isShutdown) {
            channel.shutdown()
            if (!channel.awaitTermination(5, TimeUnit.SECONDS)) {
                channel.shutdownNow()
            }
        }
    }

    override fun close() = disconnect()
}
