// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

/**
 * Represents a single Chronicle server endpoint.
 */
data class ChronicleServerAddress(val host: String, val port: Int) {
    /** Renders as `host:port`, wrapping an IPv6 [host] in bracket notation. */
    override fun toString(): String = if (host.contains(':')) "[$host]:$port" else "$host:$port"
}
