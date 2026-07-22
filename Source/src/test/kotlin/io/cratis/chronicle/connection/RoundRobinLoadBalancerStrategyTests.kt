// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RoundRobinLoadBalancerStrategyTests {

    private val addresses = listOf(
        ChronicleServerAddress("a", 1),
        ChronicleServerAddress("b", 2),
        ChronicleServerAddress("c", 3)
    )

    @Test
    fun `cycles through every address in order starting from a random offset`() = runBlocking {
        val strategy = RoundRobinLoadBalancerStrategy()
        val selections = (1..addresses.size * 2).map { strategy.select(addresses) }

        val firstIndex = addresses.indexOf(selections.first())
        val expected = selections.indices.map { addresses[(firstIndex + it) % addresses.size] }

        assertEquals(expected, selections)
    }

    @Test
    fun `returns the only address when given a single candidate`() = runBlocking {
        val strategy = RoundRobinLoadBalancerStrategy()
        val single = listOf(ChronicleServerAddress("only", 1))

        assertEquals(single.first(), strategy.select(single))
        assertEquals(single.first(), strategy.select(single))
    }

    @Test
    fun `throws when given an empty list`() {
        val strategy = RoundRobinLoadBalancerStrategy()
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { strategy.select(emptyList()) }
        }
    }
}
