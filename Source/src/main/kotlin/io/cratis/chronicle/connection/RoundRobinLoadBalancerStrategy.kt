// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * Selects addresses in rotation, starting from a random offset so that multiple client instances
 * started at the same time don't all begin with the same server.
 */
class RoundRobinLoadBalancerStrategy : LoadBalancerStrategy {
    private val nextIndex = AtomicInteger(Random.nextInt())

    override suspend fun select(addresses: List<ChronicleServerAddress>): ChronicleServerAddress {
        require(addresses.isNotEmpty()) { "Cannot select an address from an empty list." }
        return addresses[nextIndex.getAndIncrement().mod(addresses.size)]
    }
}
