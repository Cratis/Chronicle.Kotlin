// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring

import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.IChronicleClient
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.artifacts.ClientArtifacts
import io.cratis.chronicle.artifacts.IArtifactActivator
import io.cratis.chronicle.artifacts.IClientArtifacts
import io.cratis.chronicle.connection.ChronicleConnectionString
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver
import io.cratis.chronicle.sinks.WellKnownSinkTypes
import io.cratis.chronicle.spring.ChronicleProperties.NamespaceResolution.Strategy
import io.cratis.chronicle.spring.namespaces.AuthenticationNamespaceResolver
import io.cratis.chronicle.spring.namespaces.FixedNamespaceResolver
import io.cratis.chronicle.spring.namespaces.HttpHeaderNamespaceResolver
import io.cratis.chronicle.spring.namespaces.SubdomainNamespaceResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.AutoConfigurationPackages
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean

/**
 * Wires Chronicle into a Spring Boot application.
 *
 * Adding the starter to the classpath is enough: the client connects, every artifact the application
 * owns is discovered and registered with the kernel, and an `IEventStore` is ready to be injected
 * anywhere. Every bean here backs off the moment the application declares its own, so nothing has to
 * be turned off before it can be replaced.
 *
 * Web-specific behavior — per-request namespaces, identity, causation and units of work — is added by
 * [ChronicleWebAutoConfiguration] when the application is a servlet application.
 */
@AutoConfiguration
@EnableConfigurationProperties(ChronicleProperties::class)
class ChronicleAutoConfiguration {
    /**
     * Turns the `cratis.chronicle` configuration into the options the client runs on.
     *
     * When no artifact packages are configured, discovery is narrowed to the packages Spring Boot
     * already scans for components — the package of the `@SpringBootApplication` class and everything
     * beneath it. That is where an application's artifacts live, and scanning only there keeps startup
     * fast and third-party classes out of the picture.
     */
    @Bean
    @ConditionalOnMissingBean
    fun chronicleOptions(
        properties: ChronicleProperties,
        applicationContext: ApplicationContext,
        artifactActivator: IArtifactActivator,
        @Value("\${spring.application.name:Unknown}") applicationName: String
    ): ChronicleOptions = ChronicleOptions(
        connectionString = ChronicleConnectionString.parse(properties.connectionString),
        programIdentifier = properties.programIdentifier ?: applicationName,
        defaultSinkTypeId = properties.defaultSinkTypeId
            ?: System.getenv("CHRONICLE_SINK_TYPE")
            ?: WellKnownSinkTypes.MONGODB,
        autoDiscoverAndRegister = properties.autoDiscoverAndRegister,
        artifacts = clientArtifacts(properties, applicationContext),
        artifactActivator = artifactActivator
    )

    /** Lets artifacts be ordinary Spring components, with ordinary constructor injection. */
    @Bean
    @ConditionalOnMissingBean
    fun chronicleArtifactActivator(applicationContext: ApplicationContext): IArtifactActivator =
        SpringArtifactActivator(applicationContext)

    /**
     * Decides which namespace a piece of work belongs to, following `cratis.chronicle.namespace-resolution`.
     *
     * The header- and subdomain-based strategies need an HTTP request to read from and are contributed
     * by [ChronicleWebAutoConfiguration]; this one covers the strategies that work anywhere.
     */
    @Bean
    @ConditionalOnMissingBean
    fun chronicleNamespaceResolver(properties: ChronicleProperties): IEventStoreNamespaceResolver =
        when (properties.namespaceResolution.strategy) {
            Strategy.AUTHENTICATION -> AuthenticationNamespaceResolver(properties.namespaceResolution.claim)
            Strategy.HTTP_HEADER -> HttpHeaderNamespaceResolver(properties.namespaceResolution.httpHeader)
            Strategy.SUBDOMAIN -> SubdomainNamespaceResolver()
            Strategy.FIXED -> FixedNamespaceResolver(properties.namespace)
        }

    /** The connection to the kernel, closed with the application context. */
    @Bean(destroyMethod = "dispose")
    @ConditionalOnMissingBean
    fun chronicleClient(options: ChronicleOptions): IChronicleClient = ChronicleClient(options)

    /**
     * The event store for the work currently being done.
     *
     * Inject `IEventStore` anywhere — a controller, a service, a scheduled job — and it routes to the
     * right namespace on every call.
     */
    @Bean
    @ConditionalOnMissingBean
    fun chronicleEventStore(
        client: IChronicleClient,
        properties: ChronicleProperties,
        namespaceResolver: IEventStoreNamespaceResolver
    ): IEventStore = ResolvedEventStore(client, properties.eventStore, namespaceResolver)

    /** The everyday operations without coroutines — what a Java application, or a blocking MVC handler, wants. */
    @Bean
    @ConditionalOnMissingBean
    fun chronicle(eventStore: IEventStore): Chronicle = Chronicle(eventStore)

    /** Connects on startup and holds the application back until every artifact is registered. */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "cratis.chronicle", name = ["auto-discover-and-register"], havingValue = "true", matchIfMissing = true)
    fun chronicleStartup(eventStore: IEventStore, properties: ChronicleProperties): ChronicleStartup =
        ChronicleStartup(eventStore, properties.registrationTimeout)

    private fun clientArtifacts(properties: ChronicleProperties, applicationContext: ApplicationContext): IClientArtifacts {
        val packages = properties.artifactPackages.ifEmpty {
            runCatching { AutoConfigurationPackages.get(applicationContext.autowireCapableBeanFactory) }.getOrDefault(emptyList())
        }
        return ClientArtifacts(packages)
    }
}
