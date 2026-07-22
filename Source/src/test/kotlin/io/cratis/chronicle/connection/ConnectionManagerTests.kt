// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import Cratis.Chronicle.Contracts.Clients.Clients
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A kernel that pushes [keepAlives] keep-alives and then behaves according to [thenFail]:
 * either failing the stream or holding it open without sending anything more — the shape a
 * client sees when the kernel's watchdog has evicted it.
 */
private class FakeConnections(
    private val keepAlives: Int = 1,
    private val thenFail: Boolean = false
) : IKeepAliveConnections {
    var connectCount = 0
        private set
    val answered = mutableListOf<String>()

    override fun connect(request: Clients.ConnectRequest): Flow<Clients.ConnectionKeepAlive> {
        connectCount++
        return flow {
            repeat(keepAlives) {
                emit(Clients.ConnectionKeepAlive.newBuilder().setConnectionId("any").build())
            }
            if (thenFail) throw RuntimeException("unavailable") else awaitCancellation()
        }
    }

    override suspend fun answer(connectionId: String) {
        answered.add(connectionId)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionManagerTests {

    private fun TestScope.managerFor(connections: IKeepAliveConnections) = ConnectionManager(
        connections,
        dispatcher = StandardTestDispatcher(testScheduler),
        currentTimeMillis = { testScheduler.currentTime }
    )

    @Test
    fun `answers every keep-alive the kernel sends`() = runTest {
        val connections = FakeConnections(keepAlives = 3)
        val manager = managerFor(connections)

        manager.connect()
        advanceTimeBy(1_000)
        manager.close()

        // Each answer is what bumps LastSeen on the kernel; without them the client is
        // evicted and its observers unsubscribed while the stream stays open.
        assertEquals(3, connections.answered.size)
    }

    @Test
    fun `answers under the current connection id`() = runTest {
        val connections = FakeConnections(keepAlives = 1)
        val manager = managerFor(connections)
        val connectionId = manager.connectionId

        manager.connect()
        advanceTimeBy(1_000)
        manager.close()

        assertEquals(listOf(connectionId), connections.answered)
    }

    @Test
    fun `is connected once the first keep-alive arrives`() = runTest {
        val manager = managerFor(FakeConnections(keepAlives = 1))

        manager.connect()
        advanceTimeBy(1_000)
        val connected = manager.lifecycle.isConnected
        manager.close()

        assertTrue(connected)
    }

    @Test
    fun `reconnects when the stream goes silent`() = runTest {
        val connections = FakeConnections(keepAlives = 1)
        val manager = managerFor(connections)

        manager.connect()
        // The kernel does not close the stream when its watchdog evicts a client, so a
        // silent stream — not an errored one — is what a half-disconnect looks like.
        advanceTimeBy(30_000)
        manager.close()

        assertTrue(connections.connectCount >= 2, "expected a reconnect, got ${connections.connectCount}")
    }

    @Test
    fun `reconnects when the stream fails`() = runTest {
        val connections = FakeConnections(keepAlives = 0, thenFail = true)
        val manager = managerFor(connections)

        manager.connect()
        advanceTimeBy(30_000)
        manager.close()

        // A failed attempt must never end the loop — the kernel being down is the normal
        // case this exists for.
        assertTrue(connections.connectCount >= 2, "expected a retry, got ${connections.connectCount}")
    }

    @Test
    fun `uses a fresh connection id after a reconnect`() = runTest {
        val manager = managerFor(FakeConnections(keepAlives = 0, thenFail = true))
        val before = manager.connectionId

        manager.connect()
        advanceTimeBy(30_000)
        val after = manager.connectionId
        manager.close()

        // Reusing an evicted id would re-register observers the kernel has already dropped.
        assertNotEquals(before, after)
    }

    @Test
    fun `stops reporting a connection once disconnected`() = runTest {
        val manager = managerFor(FakeConnections(keepAlives = 1))

        manager.connect()
        advanceTimeBy(1_000)
        manager.disconnect()
        val connected = manager.lifecycle.isConnected
        manager.close()

        assertFalse(connected)
    }
}
