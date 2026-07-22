// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import Cratis.Chronicle.Contracts.Clients.Clients
import Cratis.Chronicle.Contracts.Clients.ConnectionServiceGrpcKt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext
import kotlin.random.Random

/**
 * Keeps the Chronicle client connected.
 *
 * The kernel pushes a keep-alive down the
 * [Connect][ConnectionServiceGrpcKt.ConnectionServiceCoroutineStub.connect] stream once
 * per second, and for each one the client must call the separate unary
 * [connectionKeepAlive][ConnectionServiceGrpcKt.ConnectionServiceCoroutineStub.connectionKeepAlive]
 * RPC back. That is what keeps the kernel counting this client as connected; a client that
 * stops answering is evicted and its observers are unsubscribed, so reactors and reducers
 * go quiet while appends keep working.
 *
 * Eviction does not close the `Connect` stream, so a lost connection usually shows up as a
 * stream that simply goes silent. A watchdog therefore treats a gap between keep-alives as
 * a lost connection, and the session is re-established either way.
 */
class ConnectionManager(
    private val connections: IKeepAliveConnections,
    val lifecycle: ConnectionLifecycle = ConnectionLifecycle(),
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    /** Stable client identity for the current connection, rotated on every reconnect. */
    val connectionId: String get() = lifecycle.connectionId

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var connectJob: Job? = null

    /** Establishes the connection and keeps re-establishing it for as long as the client lives. */
    fun connect() {
        if (connectJob?.isActive == true) return
        connectJob = scope.launch { maintainConnection() }
    }

    /** Stops connecting and cancels the keep-alive. */
    fun disconnect() {
        connectJob?.cancel()
        connectJob = null
        lifecycle.markDisconnected()
    }

    /** Releases every coroutine this connection owns. */
    fun close() {
        disconnect()
        scope.cancel()
    }

    private suspend fun maintainConnection() {
        var attempt = 0

        while (coroutineContext.isActive) {
            try {
                runSession()
                attempt = 0
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Never let a failed attempt end the loop — the kernel being down is the
                // normal case this exists for, and giving up here is what used to leave
                // the client permanently disconnected.
                System.err.println("[Chronicle] Connection lost: ${e.message}")
            }

            lifecycle.markDisconnected()
            attempt++
            delay(backoffFor(attempt))
        }
    }

    /**
     * Runs one connection: opens the stream, answers keep-alives, and returns once the
     * connection is lost. A silent stream counts as lost — see the class documentation.
     */
    private suspend fun runSession() = coroutineScope {
        val connectionId = lifecycle.connectionId
        val lastKeepAlive = AtomicLong(currentTimeMillis())

        val session = launch {
            connections.connect(buildRequest(connectionId)).collect {
                lastKeepAlive.set(currentTimeMillis())

                // Answering is what bumps LastSeen on the kernel. A failure here means the
                // connection is already gone, so let it surface rather than swallowing it.
                connections.answer(connectionId)

                lifecycle.markConnected(connectionId)
            }
        }

        val watchdog = launch {
            while (isActive) {
                delay(WATCHDOG_INTERVAL_MS)
                if (currentTimeMillis() - lastKeepAlive.get() > KEEP_ALIVE_TIMEOUT_MS) {
                    session.cancel(CancellationException("No keep-alive within ${KEEP_ALIVE_TIMEOUT_MS}ms"))
                    return@launch
                }
            }
        }

        session.join()
        watchdog.cancel()
    }

    private fun buildRequest(connectionId: String): Clients.ConnectRequest {
        val process = ProcessHandle.current()
        return Clients.ConnectRequest.newBuilder()
            .setConnectionId(connectionId)
            .setClientVersion("1.0.0")
            .setIsRunningWithDebugger(false)
            .setProcessId(process.pid().toInt())
            .setProcessPath(process.info().command().orElse(""))
            .setMachineName(InetAddress.getLocalHost().hostName)
            .setClientType(CLIENT_TYPE)
            .build()
    }

    /**
     * Exponential backoff, jittered so a fleet of clients that lost the same kernel does not
     * come back in lockstep and knock it over again.
     */
    private fun backoffFor(attempt: Int): Long {
        val ceiling = minOf(BASE_BACKOFF_MS shl (attempt - 1).coerceAtMost(MAX_BACKOFF_SHIFT), MAX_BACKOFF_MS)
        return ceiling / 2 + Random.nextLong(ceiling / 2 + 1)
    }

    companion object {
        private const val CLIENT_TYPE = "Kotlin"

        /** How often the watchdog checks that keep-alives are still arriving. */
        internal const val WATCHDOG_INTERVAL_MS = 1_000L

        /**
         * How long without a keep-alive means the connection is dead. The kernel emits one
         * per second and evicts clients whose `LastSeen` falls more than five seconds behind,
         * which is the threshold the C# client uses too.
         */
        internal const val KEEP_ALIVE_TIMEOUT_MS = 5_000L

        private const val BASE_BACKOFF_MS = 1_000L
        private const val MAX_BACKOFF_MS = 30_000L
        private const val MAX_BACKOFF_SHIFT = 16
    }
}
