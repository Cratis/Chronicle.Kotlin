// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import io.cratis.chronicle.artifacts.ArtifactActivator
import io.cratis.chronicle.artifacts.ClientArtifacts
import io.cratis.chronicle.artifacts.IArtifactActivator
import io.cratis.chronicle.artifacts.IClientArtifacts
import io.cratis.chronicle.connection.ChronicleConnectionString
import io.cratis.chronicle.readModels.DefaultReadModelNamingPolicy
import io.cratis.chronicle.readModels.ReadModelNamingPolicy
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
 * @property readModelNamingPolicy Names the container - collection or table - each read model is stored
 *   in. Defaults to [DefaultReadModelNamingPolicy], which uses the read model identifier. Set it with
 *   [withReadModelNamingPolicy]; it is deliberately not a constructor parameter, so the constructors,
 *   `copy` and `componentN` keep their published shape. Because of that a call to `copy` does not carry
 *   the policy over - set it again afterwards. It takes part in [equals] and [hashCode].
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
     * Names the container each read model is stored in. See [ReadModelNamingPolicy].
     */
    var readModelNamingPolicy: ReadModelNamingPolicy = DefaultReadModelNamingPolicy
        private set

    /**
     * The same options with automatic discovery and registration turned off, leaving every artifact to
     * be registered by hand through the services on [IEventStore].
     */
    fun withoutAutoRegistration(): ChronicleOptions =
        copy(autoDiscoverAndRegister = false).withReadModelNamingPolicy(readModelNamingPolicy)

    /**
     * The same options with [policy] naming the container each read model is stored in.
     *
     * Use it when something else reads the read model containers by name - an ORM or a query layer with
     * its own naming convention - so the client writes to the container that reader expects. Only the
     * container name changes; the read model identifier is untouched.
     *
     * @param policy The policy to apply.
     */
    fun withReadModelNamingPolicy(policy: ReadModelNamingPolicy): ChronicleOptions =
        copy().also { it.readModelNamingPolicy = policy }

    /**
     * The same options with artifact discovery narrowed to [packages] and everything beneath them.
     *
     * Worth doing in a large application: the classpath is no longer scanned end to end, and artifacts
     * belonging to third-party libraries stay out of the picture.
     *
     * @param packages The packages to scan.
     */
    fun withArtifactsFrom(vararg packages: String): ChronicleOptions =
        copy(artifacts = ClientArtifacts(packages.toList())).withReadModelNamingPolicy(readModelNamingPolicy)

    override fun equals(other: Any?): Boolean =
        other is ChronicleOptions &&
            connectionString == other.connectionString &&
            programIdentifier == other.programIdentifier &&
            defaultSinkTypeId == other.defaultSinkTypeId &&
            autoDiscoverAndRegister == other.autoDiscoverAndRegister &&
            artifacts == other.artifacts &&
            artifactActivator == other.artifactActivator &&
            openTelemetry == other.openTelemetry &&
            readModelNamingPolicy == other.readModelNamingPolicy

    override fun hashCode(): Int = listOf(
        connectionString,
        programIdentifier,
        defaultSinkTypeId,
        autoDiscoverAndRegister,
        artifacts,
        artifactActivator,
        openTelemetry,
        readModelNamingPolicy
    ).hashCode()

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
