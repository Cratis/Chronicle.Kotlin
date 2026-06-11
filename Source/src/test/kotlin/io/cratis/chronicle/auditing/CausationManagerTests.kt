// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.auditing

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class CausationManagerTests {

    private val manager = CausationManager()

    @BeforeEach
    fun reset() {
        manager.clear()
    }

    @Test
    fun `initial chain contains a root entry`() {
        val chain = manager.currentChain
        assertEquals(1, chain.size)
        assertEquals(CausationType.root, chain.first().type)
    }

    @Test
    fun `add appends an entry to the chain`() {
        manager.add(CausationType("MyCommand"), mapOf("key" to "value"))
        val chain = manager.currentChain
        assertEquals(2, chain.size)
        assertEquals("MyCommand", chain.last().type.name)
        assertEquals("value", chain.last().properties["key"])
    }

    @Test
    fun `defineRoot replaces the first entry`() {
        manager.defineRoot(mapOf("source" to "test"))
        val chain = manager.currentChain
        assertEquals(1, chain.size)
        assertEquals(CausationType.root, chain.first().type)
        assertEquals("test", chain.first().properties["source"])
    }

    @Test
    fun `clear resets chain to a single root entry`() {
        manager.add(CausationType("Cmd1"))
        manager.add(CausationType("Cmd2"))
        manager.clear()
        val chain = manager.currentChain
        assertEquals(1, chain.size)
        assertEquals(CausationType.root, chain.first().type)
    }

    @Test
    fun `chains are independent per thread`() {
        manager.add(CausationType("MainThread"))

        var otherChainSize = -1
        val thread = Thread {
            otherChainSize = manager.currentChain.size
        }
        thread.start()
        thread.join()

        assertEquals(1, otherChainSize)
        assertEquals(2, manager.currentChain.size)
    }
}
