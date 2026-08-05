// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

/** The address-selection strategy to use when a connection string resolves to more than one server. */
enum class LoadBalancer {
    LEAST_CONNECTIONS,
    ROUND_ROBIN,
    RANDOM;

    companion object {
        /** Parses the value of a `loadBalancer` connection string option. */
        fun parse(value: String): LoadBalancer = when (value.lowercase()) {
            "least-connections" -> LEAST_CONNECTIONS
            "round-robin" -> ROUND_ROBIN
            "random" -> RANDOM
            else -> throw IllegalArgumentException("Unknown loadBalancer strategy: '$value'.")
        }
    }

    /** Renders this strategy as the `loadBalancer` connection string option value [parse] understands. */
    fun toConnectionStringValue(): String = name.lowercase().replace('_', '-')
}
