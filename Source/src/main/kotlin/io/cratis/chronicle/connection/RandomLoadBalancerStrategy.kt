// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

/** Selects a uniformly random address on every call. */
class RandomLoadBalancerStrategy : LoadBalancerStrategy {
    override suspend fun select(addresses: List<ChronicleServerAddress>): ChronicleServerAddress {
        require(addresses.isNotEmpty()) { "Cannot select an address from an empty list." }
        return addresses.random()
    }
}
