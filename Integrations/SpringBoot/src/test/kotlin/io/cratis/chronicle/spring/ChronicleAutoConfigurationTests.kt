// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring

import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.IChronicleClient
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.artifacts.IArtifactActivator
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver
import io.cratis.chronicle.spring.namespaces.AuthenticationNamespaceResolver
import io.cratis.chronicle.spring.namespaces.FixedNamespaceResolver
import io.cratis.chronicle.spring.namespaces.HttpHeaderNamespaceResolver
import io.cratis.chronicle.spring.namespaces.SubdomainNamespaceResolver
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class ChronicleAutoConfigurationTests {

    // A stand-in client keeps the context from dialling a kernel that is not there, while leaving every
    // other bean to be built exactly as it would be in a real application.
    @Configuration(proxyBeanMethods = false)
    class WithoutAKernel {
        @Bean
        fun client(): IChronicleClient = mockk(relaxed = true)
    }

    private val runner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ChronicleAutoConfiguration::class.java))
        .withUserConfiguration(WithoutAKernel::class.java)

    @Test
    fun `gives an application everything it needs without any configuration at all`() {
        runner.run { context ->
            assertThat(context).hasSingleBean(ChronicleOptions::class.java)
            assertThat(context).hasSingleBean(IEventStore::class.java)
            assertThat(context).hasSingleBean(Chronicle::class.java)
            assertThat(context).hasSingleBean(IArtifactActivator::class.java)
            assertThat(context).hasSingleBean(IEventStoreNamespaceResolver::class.java)
        }
    }

    @Test
    fun `discovers and registers artifacts automatically by default`() {
        runner.run { context ->
            assertThat(context.getBean(ChronicleOptions::class.java).autoDiscoverAndRegister).isTrue()
            assertThat(context).hasSingleBean(ChronicleStartup::class.java)
        }
    }

    @Test
    fun `leaves registration alone once told to`() {
        runner.withPropertyValues("cratis.chronicle.auto-discover-and-register=false").run { context ->
            assertThat(context.getBean(ChronicleOptions::class.java).autoDiscoverAndRegister).isFalse()
            assertThat(context).doesNotHaveBean(ChronicleStartup::class.java)
        }
    }

    @Test
    fun `takes the connection string and event store from configuration`() {
        runner.withPropertyValues(
            "cratis.chronicle.connection-string=chronicle://kernel.example.com:36000",
            "cratis.chronicle.event-store=Ordering"
        ).run { context ->
            val options = context.getBean(ChronicleOptions::class.java)
            assertThat(options.connectionString.host).isEqualTo("kernel.example.com")
            assertThat(options.connectionString.port).isEqualTo(36000)
            assertThat(context.getBean(ChronicleProperties::class.java).eventStore).isEqualTo("Ordering")
        }
    }

    @Test
    fun `names the program after the application unless told otherwise`() {
        runner.withPropertyValues("spring.application.name=ordering").run { context ->
            assertThat(context.getBean(ChronicleOptions::class.java).programIdentifier).isEqualTo("ordering")
        }
    }

    @Test
    fun `resolves everything to one namespace by default`() {
        runner.run { context ->
            val resolver = context.getBean(IEventStoreNamespaceResolver::class.java)
            assertThat(resolver).isInstanceOf(FixedNamespaceResolver::class.java)
            assertThat(resolver.resolve()).isEqualTo("Default")
        }
    }

    @Test
    fun `uses the configured namespace for the fixed strategy`() {
        runner.withPropertyValues("cratis.chronicle.namespace=acme").run { context ->
            assertThat(context.getBean(IEventStoreNamespaceResolver::class.java).resolve()).isEqualTo("acme")
        }
    }

    @Test
    fun `picks the namespace resolver named by the strategy`() {
        runner.withPropertyValues("cratis.chronicle.namespace-resolution.strategy=http_header").run { context ->
            assertThat(context.getBean(IEventStoreNamespaceResolver::class.java))
                .isInstanceOf(HttpHeaderNamespaceResolver::class.java)
        }
        runner.withPropertyValues("cratis.chronicle.namespace-resolution.strategy=subdomain").run { context ->
            assertThat(context.getBean(IEventStoreNamespaceResolver::class.java))
                .isInstanceOf(SubdomainNamespaceResolver::class.java)
        }
        runner.withPropertyValues("cratis.chronicle.namespace-resolution.strategy=authentication").run { context ->
            assertThat(context.getBean(IEventStoreNamespaceResolver::class.java))
                .isInstanceOf(AuthenticationNamespaceResolver::class.java)
        }
    }

    @Test
    fun `steps aside for an application's own namespace resolver`() {
        runner.withBean(IEventStoreNamespaceResolver::class.java, { IEventStoreNamespaceResolver { "mine" } })
            .run { context ->
                assertThat(context.getBean(IEventStoreNamespaceResolver::class.java).resolve()).isEqualTo("mine")
            }
    }

    @Test
    fun `steps aside for an application's own options`() {
        val ours = ChronicleOptions.development().withoutAutoRegistration()
        runner.withBean(ChronicleOptions::class.java, { ours }).run { context ->
            assertThat(context.getBean(ChronicleOptions::class.java)).isSameAs(ours)
        }
    }

    @Test
    fun `activates artifacts through the Spring container`() {
        runner.run { context ->
            assertThat(context.getBean(IArtifactActivator::class.java))
                .isInstanceOf(SpringArtifactActivator::class.java)
        }
    }
}
