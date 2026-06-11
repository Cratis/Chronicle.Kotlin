// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import io.cratis.chronicle.connection.ChronicleConnectionString

/**
 * Options used to configure a [ChronicleClient].
 *
 * @property connectionString The parsed [ChronicleConnectionString] for the server.
 * @property programIdentifier A human-readable identifier for the connecting program. Used in diagnostics.
 */
data class ChronicleOptions(
    val connectionString: ChronicleConnectionString,
    val programIdentifier: String = "Unknown"
) {
    companion object {
        /**
         * Creates [ChronicleOptions] from a raw connection string.
         *
         * @param connectionString A `chronicle://` connection string.
         * @return The resulting [ChronicleOptions].
         */
        fun fromConnectionString(connectionString: String): ChronicleOptions =
            ChronicleOptions(ChronicleConnectionString.parse(connectionString))

        /**
         * Creates [ChronicleOptions] pre-configured for local development.
         *
         * Points to localhost:35000 with TLS disabled and the standard dev credentials.
         */
        fun development(): ChronicleOptions =
            ChronicleOptions(ChronicleConnectionString.DEVELOPMENT)
    }
}
