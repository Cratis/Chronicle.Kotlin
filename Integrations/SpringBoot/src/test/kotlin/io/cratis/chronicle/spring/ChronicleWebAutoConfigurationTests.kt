// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring

import io.cratis.chronicle.IChronicleClient
import io.cratis.chronicle.spring.auditing.CausationFilter
import io.cratis.chronicle.spring.identity.IdentityFilter
import io.cratis.chronicle.spring.transactions.UnitOfWorkFilter
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class ChronicleWebAutoConfigurationTests {

    @Configuration(proxyBeanMethods = false)
    class WithoutAKernel {
        @Bean
        fun client(): IChronicleClient = mockk(relaxed = true)
    }

    private val autoConfigurations = AutoConfigurations.of(
        ChronicleAutoConfiguration::class.java,
        ChronicleWebAutoConfiguration::class.java
    )

    private val web = WebApplicationContextRunner()
        .withConfiguration(autoConfigurations)
        .withUserConfiguration(WithoutAKernel::class.java)

    @Test
    fun `a web application gets identity, causation and units of work per request`() {
        web.run { context ->
            assertThat(context).hasSingleBean(UnitOfWorkFilter::class.java)
            assertThat(context).hasSingleBean(CausationFilter::class.java)
            assertThat(context).hasSingleBean(IdentityFilter::class.java)
        }
    }

    @Test
    fun `each piece can be turned off on its own`() {
        web.withPropertyValues("cratis.chronicle.unit-of-work.enabled=false").run { context ->
            assertThat(context).doesNotHaveBean(UnitOfWorkFilter::class.java)
            assertThat(context).hasSingleBean(CausationFilter::class.java)
        }
        web.withPropertyValues("cratis.chronicle.causation.enabled=false").run { context ->
            assertThat(context).doesNotHaveBean(CausationFilter::class.java)
        }
        web.withPropertyValues("cratis.chronicle.identity.enabled=false").run { context ->
            assertThat(context).doesNotHaveBean(IdentityFilter::class.java)
        }
    }

    @Test
    fun `an application that serves no requests gets none of it`() {
        ApplicationContextRunner()
            .withConfiguration(autoConfigurations)
            .withUserConfiguration(WithoutAKernel::class.java)
            .run { context ->
                assertThat(context).doesNotHaveBean(UnitOfWorkFilter::class.java)
                assertThat(context).doesNotHaveBean(CausationFilter::class.java)
                assertThat(context).doesNotHaveBean(IdentityFilter::class.java)
            }
    }

    @Test
    fun `filters are ordered so identity and causation are in place before anything is staged`() {
        web.run { context ->
            val identity = context.getBean(IdentityFilter::class.java).order
            val causation = context.getBean(CausationFilter::class.java).order
            val unitOfWork = context.getBean(UnitOfWorkFilter::class.java).order

            assertThat(identity).isLessThan(causation)
            assertThat(causation).isLessThan(unitOfWork)
        }
    }
}
