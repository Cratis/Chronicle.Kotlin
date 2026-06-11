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
}
