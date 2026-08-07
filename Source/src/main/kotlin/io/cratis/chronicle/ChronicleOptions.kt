// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import io.cratis.chronicle.artifacts.ArtifactActivator
import io.cratis.chronicle.artifacts.ClientArtifacts
import io.cratis.chronicle.artifacts.IArtifactActivator
import io.cratis.chronicle.artifacts.IClientArtifacts
import io.cratis.chronicle.connection.ChronicleConnectionString
import io.cratis.chronicle.sinks.WellKnownSinkTypes
import io.opentelemetry.api.OpenTelemetry

/**
 * Options used to configure a [ChronicleClient].
 *
 * @property connectionString The parsed [ChronicleConnectionString] for the server.
 * @property programIdentifier A human-readable identifier for the connecting program. Used in diagnostics.
 * @property defaultSinkTypeId The sink type used when registering reducers and projections.
 *   Defaults to [WellKnownSinkTypes.MONGODB]. Override by passing an explicit value or by setting
 *   the `CHRONICLE_SINK_TYPE` environment variable (e.g. `CHRONICLE_SINK_TYPE=SQL`).
 * @property autoDiscoverAndRegister Whether every artifact found by [artifacts] is registered with the
 *   kernel automatically as soon as the client connects. On by default — turn it off to register
 *   artifacts by hand through the services on [IEventStore].
 * @property artifacts What the application consists of. Defaults to scanning the classpath; narrow it
 *   with [ClientArtifacts] or list artifacts explicitly with
 *   [io.cratis.chronicle.artifacts.KnownClientArtifacts].
 * @property artifactActivator Creates the instances for discovered artifacts. Replace it to let a
 *   dependency injection container construct them.
 * @property openTelemetry Where the client's spans go. Defaults to `null`, meaning whatever the
 *   application registered globally — which is a no-op until an application installs an SDK, so a
 *   client that is never instrumented produces nothing. Set this when the application holds its own
 *   [OpenTelemetry] rather than registering it globally.
 */
data class ChronicleOptions @JvmOverloads constructor(
    val connectionString: ChronicleConnectionString,
    val programIdentifier: String = "Unknown",
    val defaultSinkTypeId: String = System.getenv("CHRONICLE_SINK_TYPE") ?: WellKnownSinkTypes.MONGODB,
    val autoDiscoverAndRegister: Boolean = true,
    val artifacts: IClientArtifacts = ClientArtifacts.default,
    val artifactActivator: IArtifactActivator = ArtifactActivator,
    val openTelemetry: OpenTelemetry? = null
) {
    /**
     * The same options with automatic discovery and registration turned off, leaving every artifact to
     * be registered by hand through the services on [IEventStore].
     */
    fun withoutAutoRegistration(): ChronicleOptions = copy(autoDiscoverAndRegister = false)

    /**
     * The same options with artifact discovery narrowed to [packages] and everything beneath them.
     *
     * Worth doing in a large application: the classpath is no longer scanned end to end, and artifacts
     * belonging to third-party libraries stay out of the picture.
     *
     * @param packages The packages to scan.
     */
    fun withArtifactsFrom(vararg packages: String): ChronicleOptions =
        copy(artifacts = ClientArtifacts(packages.toList()))

    companion object {
        /**
         * Creates [ChronicleOptions] from a raw connection string.
         *
         * @param connectionString A `chronicle://` connection string.
         * @return The resulting [ChronicleOptions].
         */
        @JvmStatic
        fun fromConnectionString(connectionString: String): ChronicleOptions =
            ChronicleOptions(ChronicleConnectionString.parse(connectionString))

        /**
         * Creates [ChronicleOptions] pre-configured for local development.
         *
         * Points to localhost:35000 over TLS with the standard dev credentials.
         */
        @JvmStatic
        fun development(): ChronicleOptions =
            ChronicleOptions(ChronicleConnectionString.DEVELOPMENT)
    }
}
