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

    init {
        require(connectionString.apiKey == null) {
            "API key authentication is not supported by the Chronicle kernel; use a client id and secret in the connection string."
        }
    }

    private val srvResolver = SrvResolver()
    private val loadBalancerStrategy = LoadBalancerStrategy.forConnectionString(connectionString)

    /**
     * The channel every stub holds. The [SwappableChannel] indirection is what makes
     * [rebuildChannel] possible: stubs live as long as this connection, while the managed
     * channel underneath is replaced whenever the session drops.
     */
    private val channelDelegate = lazy {
        SwappableChannel(runBlocking(Dispatchers.IO) { dial() })
    }
    private val channel: SwappableChannel by channelDelegate

    val services: ChronicleServices by lazy { ChronicleServices(channel) }

    private val connectionManagerDelegate = lazy {
        ConnectionManager(
            KeepAliveConnections(services.connection),
            refreshChannel = { rebuildChannel() }
        ).also { it.connect() }
    }
    private val connectionManager: ConnectionManager by connectionManagerDelegate

    /** Tracks whether the client is connected, and under which connection ID. */
    val lifecycle: ConnectionLifecycle get() = connectionManager.lifecycle

    /** The current client connection ID. Rotates on every reconnect. */
    val connectionId: String get() = connectionManager.connectionId

    /**
     * Dials a fresh managed channel for a newly resolved and selected server address.
     *
     * Every dial resolves again rather than reusing an earlier result: a `chronicle+srv://`
     * connection string is looked up via DNS through [srvResolver], then
     * [loadBalancerStrategy] picks one address from the result (or from the connection
     * string's explicit [ChronicleConnectionString.addresses] for a non-SRV, possibly
     * multi-host, connection string). A server that comes up, goes down, or a DNS record
     * that changes is therefore picked up on the next dial, and the OAuth token endpoint
     * follows the dialed address.
     */
    private suspend fun dial(): ManagedChannel {
        val address = resolveAndSelect()
        val builder = Grpc.newChannelBuilderForAddress(
            address.host,
            address.port,
            connectionString.createCredentials()
        )
        builder.intercept(BearerTokenInterceptor(createTokenProvider(address)))
        val managed = builder.build()
        try {
            CompatibilityPreflight.verify(managed)
            return managed
        } catch (error: Throwable) {
            managed.shutdownNow()
            throw error
        }
    }

    /**
     * Drops the current channel and dials a fresh one.
     *
     * Called by [ConnectionManager] before every reconnect attempt: the session that ran
     * on the previous channel is gone, and retrying on it could fail identically forever —
     * the address it dials was pinned when it was built.
     */
    private suspend fun rebuildChannel() {
        val previous = channel.swap(dial())
        // The kernel has already evicted the session that ran on it and dropped its
        // observers — nothing on the previous channel is worth draining.
        previous.shutdownNow()
    }

    private suspend fun resolveAndSelect(): ChronicleServerAddress {
        val addresses = if (connectionString.isSrv) {
            srvResolver.resolve(connectionString.host, connectionString.srvNameServer)
        } else {
            connectionString.addresses
        }
        return loadBalancerStrategy.select(addresses)
    }

    private fun createTokenProvider(address: ChronicleServerAddress): ITokenProvider {
        val username = connectionString.username
            ?: ChronicleConnectionString.DEVELOPMENT_CLIENT
        val password = connectionString.password
            ?: ChronicleConnectionString.DEVELOPMENT_CLIENT_SECRET

        val scheme = if (connectionString.disableTls) "http" else "https"
        val tokenEndpoint =
            "$scheme://${address.host}:${address.port}/connect/token"

        return OAuthTokenProvider(
            tokenEndpoint,
            username,
            password,
            connectionString.disableTls,
            connectionString.skipTlsValidation
        )
    }

    fun connect() {
        @Suppress("UNUSED_EXPRESSION")
        connectionManager
    }

    fun disconnect() {
        if (connectionManagerDelegate.isInitialized()) connectionManager.close()
        if (!channelDelegate.isInitialized()) return
        val managed = channel.current
        if (!managed.isShutdown) {
            managed.shutdown()
            if (!managed.awaitTermination(5, TimeUnit.SECONDS)) {
                managed.shutdownNow()
            }
        }
    }

    override fun close() = disconnect()
}
