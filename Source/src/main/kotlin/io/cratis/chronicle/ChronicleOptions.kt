// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import io.cratis.chronicle.connection.ChronicleConnectionString
import io.cratis.chronicle.sinks.WellKnownSinkTypes

/**
 * Options used to configure a [ChronicleClient].
 *
 * @property connectionString The parsed [ChronicleConnectionString] for the server.
 * @property programIdentifier A human-readable identifier for the connecting program. Used in diagnostics.
 * @property defaultSinkTypeId The sink type used when registering reducers and projections.
 *   Defaults to [WellKnownSinkTypes.MONGODB]. Override by passing an explicit value or by setting
 *   the `CHRONICLE_SINK_TYPE` environment variable (e.g. `CHRONICLE_SINK_TYPE=SQL`).
 */
data class ChronicleOptions(
    val connectionString: ChronicleConnectionString,
    val programIdentifier: String = "Unknown",
    val defaultSinkTypeId: String = System.getenv("CHRONICLE_SINK_TYPE") ?: WellKnownSinkTypes.MONGODB
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
