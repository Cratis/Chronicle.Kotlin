// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionLifecycleTests {

    @Test
    fun `starts out disconnected`() {
        val lifecycle = ConnectionLifecycle()

        assertFalse(lifecycle.isConnected)
    }

    @Test
    fun `is connected once the kernel has acknowledged the connection`() {
        val lifecycle = ConnectionLifecycle()

        lifecycle.markConnected(lifecycle.connectionId)

        assertTrue(lifecycle.isConnected)
    }

    @Test
    fun `rotates the connection id on disconnect`() {
        val lifecycle = ConnectionLifecycle()
        val before = lifecycle.connectionId
        lifecycle.markConnected(before)

        lifecycle.markDisconnected()

        // The kernel keys observer subscriptions by connection id and drops them when it
        // evicts a client, so reusing an evicted id would re-register a ghost.
        assertNotEquals(before, lifecycle.connectionId)
        assertFalse(lifecycle.isConnected)
    }

    @Test
    fun `emits every connection so observers can re-register`() = runTest {
        val lifecycle = ConnectionLifecycle()
        val seen = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            lifecycle.connections().toList(seen)
        }

        val first = lifecycle.connectionId
        lifecycle.markConnected(first)
        lifecycle.markDisconnected()
        val second = lifecycle.connectionId
        lifecycle.markConnected(second)

        assertEquals(listOf(first, second), seen)
    }

    @Test
    fun `does not emit the same connection twice`() = runTest {
        val lifecycle = ConnectionLifecycle()
        val seen = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            lifecycle.connections().toList(seen)
        }

        val connectionId = lifecycle.connectionId
        lifecycle.markConnected(connectionId)
        lifecycle.markConnected(connectionId)

        // Re-emitting would make every observer tear down and re-register for nothing.
        assertEquals(listOf(connectionId), seen)
    }
}
