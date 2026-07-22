// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RandomLoadBalancerStrategyTests {

    private val addresses = listOf(
        ChronicleServerAddress("a", 1),
        ChronicleServerAddress("b", 2),
        ChronicleServerAddress("c", 3)
    )

    @Test
    fun `always selects one of the given addresses`() = runBlocking {
        val strategy = RandomLoadBalancerStrategy()
        repeat(50) {
            assertTrue(strategy.select(addresses) in addresses)
        }
    }

    @Test
    fun `returns the only address when given a single candidate`() = runBlocking {
        val strategy = RandomLoadBalancerStrategy()
        val single = listOf(ChronicleServerAddress("only", 1))
        assertTrue(strategy.select(single) == single.first())
    }

    @Test
    fun `throws when given an empty list`() {
        val strategy = RandomLoadBalancerStrategy()
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { strategy.select(emptyList()) }
        }
    }

    @Test
    fun `eventually selects every address given enough attempts`() = runBlocking {
        val strategy = RandomLoadBalancerStrategy()
        val selected = (1..200).map { strategy.select(addresses) }.toSet()
        assertTrue(selected.containsAll(addresses))
    }
}
