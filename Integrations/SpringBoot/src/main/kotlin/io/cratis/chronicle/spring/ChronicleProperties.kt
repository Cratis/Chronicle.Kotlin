// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring

import io.cratis.chronicle.EventStoreNamespaceName
import io.cratis.chronicle.connection.ChronicleConnectionString
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Everything Chronicle needs to know, expressed as ordinary Spring Boot configuration under the
 * `cratis.chronicle` prefix.
 *
 * The defaults are the ones a developer wants on their own machine: the local development kernel, a
 * single `Default` namespace, and automatic registration of every artifact on the classpath. A minimal
 * `application.yml` therefore only has to name the event store:
 *
 * ```yaml
 * cratis:
 *   chronicle:
 *     event-store: Ordering
 * ```
 *
 * @property connectionString Where the kernel is. Defaults to the local development kernel.
 * @property eventStore The event store this application works against.
 * @property namespace The namespace used when namespace resolution is [NamespaceResolution.Strategy.FIXED].
 * @property autoDiscoverAndRegister Whether artifacts are registered with the kernel automatically on
 *   connect. Turn it off to register everything by hand.
 * @property artifactPackages Packages to scan for artifacts. Defaults to the packages of the
 *   application's `@SpringBootApplication` class, which is what nearly every application wants.
 * @property defaultSinkTypeId The sink read models are persisted to. Defaults to MongoDB.
 * @property programIdentifier How this program identifies itself in diagnostics. Defaults to the
 *   Spring application name.
 * @property registrationTimeout How long startup waits for artifacts to be registered before carrying
 *   on regardless, so an unreachable kernel degrades rather than blocks.
 * @property namespaceResolution How the namespace for a piece of work is decided.
 */
@ConfigurationProperties(prefix = "cratis.chronicle")
class ChronicleProperties {
    var connectionString: String = ChronicleConnectionString.DEVELOPMENT.toString()
    var eventStore: String = "Default"
    var namespace: String = EventStoreNamespaceName.default.value
    var autoDiscoverAndRegister: Boolean = true
    var artifactPackages: List<String> = emptyList()
    var defaultSinkTypeId: String? = null
    var programIdentifier: String? = null
    var registrationTimeout: Duration = Duration.ofSeconds(30)
    var namespaceResolution: NamespaceResolution = NamespaceResolution()

    /**
     * How the namespace for a piece of work is decided.
     *
     * @property strategy Which of the built-in resolvers to use.
     * @property httpHeader The header read by [Strategy.HTTP_HEADER].
     * @property claim The claim or authority prefix read by [Strategy.AUTHENTICATION].
     */
    class NamespaceResolution {
        var strategy: Strategy = Strategy.FIXED
        var httpHeader: String = "x-cratis-tenant-id"
        var claim: String = "tenant_id"

        /** The built-in ways of deciding which namespace a piece of work belongs to. */
        enum class Strategy {
            /** Everything goes to [ChronicleProperties.namespace]. The right choice for a single-tenant application. */
            FIXED,

            /** The namespace comes from an HTTP header on the current request. */
            HTTP_HEADER,

            /** The namespace is the subdomain of the current request's host — `acme` in `acme.example.com`. */
            SUBDOMAIN,

            /** The namespace comes from a claim on the currently authenticated principal. */
            AUTHENTICATION
        }
    }

}
