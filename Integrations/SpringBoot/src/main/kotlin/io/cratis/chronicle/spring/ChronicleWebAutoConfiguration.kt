// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring

import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.spring.auditing.CausationFilter
import io.cratis.chronicle.spring.identity.IdentityFilter
import io.cratis.chronicle.spring.transactions.UnitOfWorkFilter
import jakarta.servlet.Filter
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Adds the per-request behavior a web application wants from Chronicle: who did it, what they were
 * doing, and all of it committed together.
 *
 * Everything here is a plain servlet filter, so it applies to controllers, functional endpoints and
 * anything else served over HTTP without application code opting in. Each piece can be switched off on
 * its own through `cratis.chronicle.*.enabled`.
 */
@AutoConfiguration(after = [ChronicleAutoConfiguration::class])
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(Filter::class)
@EnableConfigurationProperties(ChronicleProperties::class)
class ChronicleWebAutoConfiguration {
    /** Runs each request inside a unit of work, so everything a handler appends lands together. */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "cratis.chronicle.unit-of-work", name = ["enabled"], havingValue = "true", matchIfMissing = true)
    fun chronicleUnitOfWorkFilter(eventStore: IEventStore): UnitOfWorkFilter = UnitOfWorkFilter(eventStore)

    /** Records the request that led to an event on the event itself. */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "cratis.chronicle.causation", name = ["enabled"], havingValue = "true", matchIfMissing = true)
    fun chronicleCausationFilter(): CausationFilter = CausationFilter()

    /**
     * Identity lives in its own configuration class so that neither it nor [IdentityFilter] is loaded
     * at all in an application without Spring Security.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = ["org.springframework.security.core.context.SecurityContextHolder"])
    class Identity {
        /** Records the authenticated user as the identity behind every event appended during the request. */
        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnProperty(prefix = "cratis.chronicle.identity", name = ["enabled"], havingValue = "true", matchIfMissing = true)
        fun chronicleIdentityFilter(): IdentityFilter = IdentityFilter()
    }
}
