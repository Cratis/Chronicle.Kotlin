// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID

/**
 * The state of a Chronicle connection at a point in time.
 *
 * @property connectionId The client identity the kernel knows this connection by.
 * @property isConnected Whether the kernel has acknowledged the connection.
 */
data class ConnectionState(val connectionId: String, val isConnected: Boolean)

/**
 * Tracks whether the client is connected and under which identity, and lets observers
 * follow that.
 *
 * The connection id is **rotated on every disconnect**. The kernel keys observer
 * subscriptions by connection id and evicts a connection it stops hearing from, so
 * reusing an evicted id after a reconnect would leave the client registered as a ghost
 * whose observations never fire. Every reconnect therefore re-registers under a fresh id.
 */
class ConnectionLifecycle {
    private val _state = MutableStateFlow(ConnectionState(newConnectionId(), false))

    /** The current connection state, and every change to it. */
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    /** The connection id currently in use. */
    val connectionId: String get() = _state.value.connectionId

    /** Whether the kernel has acknowledged the connection. */
    val isConnected: Boolean get() = _state.value.isConnected

    /**
     * Emits the connection id of each successive connection, so an observer can
     * re-register itself every time the client comes back.
     */
    fun connections(): Flow<String> = state
        .filter { it.isConnected }
        .map { it.connectionId }
        .distinctUntilChanged()

    /** Marks the connection as acknowledged by the kernel under [connectionId]. */
    fun markConnected(connectionId: String) {
        _state.update { current ->
            if (current.isConnected && current.connectionId == connectionId) {
                current
            } else {
                ConnectionState(connectionId, true)
            }
        }
    }

    /** Marks the connection as lost and rotates the connection id. */
    fun markDisconnected() {
        _state.update { ConnectionState(newConnectionId(), false) }
    }

    private companion object {
        fun newConnectionId(): String = UUID.randomUUID().toString()
    }
}
