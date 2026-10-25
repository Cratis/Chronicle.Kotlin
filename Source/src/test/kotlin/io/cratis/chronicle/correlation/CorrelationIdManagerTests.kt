// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.correlation

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class CorrelationIdManagerTests {

    private val manager = CorrelationIdManager()

    @BeforeEach
    fun reset() {
        manager.clear()
    }

    @Test
    fun `current returns a non-null UUID`() {
        assertNotNull(manager.current)
    }

    @Test
    fun `current returns the same UUID on repeated calls in the same thread`() {
        val first = manager.current
        val second = manager.current
        assertEquals(first, second)
    }

    @Test
    fun `set changes the current UUID`() {
        val newId = java.util.UUID.randomUUID()
        manager.set(newId)
        assertEquals(newId, manager.current)
    }

    @Test
    fun `clear causes a new UUID to be generated on next access`() {
        val original = manager.current
        manager.clear()
        val next = manager.current
        assertNotEquals(original, next)
    }

    @Test
    fun `UUIDs are independent per thread`() {
        val mainId = manager.current
        var threadId: java.util.UUID? = null
        val thread = Thread { threadId = manager.current }
        thread.start()
        thread.join()
        assertNotNull(threadId)
        assertNotEquals(mainId, threadId)
    }

    @Test
    fun `separately constructed managers are isolated from each other and the singleton`() {
        val other = CorrelationIdManager()
        val id = java.util.UUID.randomUUID()
        manager.set(id)
        assertNotEquals(id, other.current)
        assertNotEquals(id, correlationIdManager.current)
        assertNotSame(manager, correlationIdManager)
        other.clear()
    }

    @Test
    fun `explicit zero uuid survives reads`() {
        val zero = java.util.UUID(0L, 0L)
        manager.set(zero)
        assertEquals(zero, manager.current)
        assertEquals(zero, manager.current)
    }

    @Test
    fun `worker thread set clear and reuse leave fresh state and do not leak to the caller`() {
        val mainId = manager.current
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        try {
            val inherited = executor.submit<java.util.UUID> { manager.current }.get()
            assertNotEquals(mainId, inherited)
            val explicit = java.util.UUID.randomUUID()
            val reused = executor.submit<List<java.util.UUID>> {
                manager.set(explicit)
                val set = manager.current
                manager.clear()
                listOf(set, manager.current)
            }.get()
            assertEquals(explicit, reused[0])
            assertNotEquals(explicit, reused[1])
            assertNotEquals(inherited, reused[1])
        } finally {
            executor.shutdownNow()
        }
        assertEquals(mainId, manager.current)
    }

    @Test
    fun `java callers can use the manager and the module singleton`() {
        val id = java.util.UUID.randomUUID()
        val javaManager = JavaCorrelationUsage.create()
        assertEquals(id, JavaCorrelationUsage.roundTrip(javaManager, id))
        assertEquals(JavaCorrelationUsage.current(javaManager), JavaCorrelationUsage.current(javaManager))
        assertSame(correlationIdManager, JavaCorrelationUsage.singleton())
    }
}
