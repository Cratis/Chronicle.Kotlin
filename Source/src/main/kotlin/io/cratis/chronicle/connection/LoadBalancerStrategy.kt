// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

/** Selects a single server address to connect to from a resolved set of Chronicle server addresses. */
interface LoadBalancerStrategy {
    /** Selects one address from [addresses], which is never empty. */
    suspend fun select(addresses: List<ChronicleServerAddress>): ChronicleServerAddress

    companion object {
        /** Builds the [LoadBalancerStrategy] configured on [connectionString]. */
        fun forConnectionString(connectionString: ChronicleConnectionString): LoadBalancerStrategy =
            when (connectionString.loadBalancer) {
                LoadBalancer.LEAST_CONNECTIONS ->
                    LeastConnectionsLoadBalancerStrategy(connectionString.disableTls, connectionString.skipTlsValidation)
                LoadBalancer.ROUND_ROBIN -> RoundRobinLoadBalancerStrategy()
                LoadBalancer.RANDOM -> RandomLoadBalancerStrategy()
            }
    }
}
