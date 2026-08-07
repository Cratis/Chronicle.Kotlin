// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring

import io.cratis.chronicle.EventStore
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.auditing.causationManager
import io.cratis.chronicle.connection.ChronicleServices
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.identity.Identity
import io.cratis.chronicle.identity.identityProvider
import io.cratis.chronicle.spring.auditing.CausationFilter
import io.cratis.chronicle.spring.identity.IdentityFilter
import io.cratis.chronicle.spring.transactions.UnitOfWorkFilter
import io.grpc.Grpc
import io.grpc.InsecureChannelCredentials
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class FiltersTests {

    private val request = MockHttpServletRequest("POST", "/api/employees/1/hire").apply {
        serverName = "acme.example.com"
        scheme = "https"
        queryString = "notify=true"
    }
    private val response = MockHttpServletResponse()

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
        identityProvider.clearCurrentIdentity()
        causationManager.clear()
    }

    /** Captures whatever the surrounding filter had set up by the time the handler would have run. */
    private class Observing(val observe: () -> Unit) : FilterChain {
        override fun doFilter(request: jakarta.servlet.ServletRequest, response: jakarta.servlet.ServletResponse) = observe()
    }

    // ---- identity -------------------------------------------------------------------------------

    @Test
    fun `identity filter makes the authenticated caller the identity for the request`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(PrincipalWithClaims(mapOf("sub" to "u-1", "name" to "Ada Lovelace")), "x", emptyList())
        var during = Identity.notSet

        IdentityFilter().doFilter(request, response, Observing { during = identityProvider.currentIdentity })

        assertThat(during.subject).isEqualTo("u-1")
        assertThat(during.name).isEqualTo("Ada Lovelace")
    }

    @Test
    fun `identity filter clears the identity so a pooled thread never inherits it`() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(PrincipalWithClaims(mapOf("sub" to "u-1")), "x", emptyList())

        IdentityFilter().doFilter(request, response, MockFilterChain())

        assertThat(identityProvider.currentIdentity).isEqualTo(Identity.system)
    }

    // ---- causation ------------------------------------------------------------------------------

    @Test
    fun `causation filter records the request on the causation chain`() {
        var during = emptyList<io.cratis.chronicle.auditing.Causation>()

        CausationFilter().doFilter(request, response, Observing { during = causationManager.currentChain })

        assertThat(during.map { it.type }).contains(CausationFilter.CAUSATION_TYPE)
        assertThat(during.first().properties["route"]).isEqualTo("/api/employees/1/hire")
        assertThat(during.first().properties["method"]).isEqualTo("POST")
        assertThat(during.first().properties["host"]).isEqualTo("acme.example.com")
        assertThat(during.first().properties["scheme"]).isEqualTo("https")
        assertThat(during.first().properties["query"]).isEqualTo("notify=true")
    }

    @Test
    fun `causation filter clears the chain when the request is done`() {
        CausationFilter().doFilter(request, response, MockFilterChain())

        assertThat(causationManager.currentChain.map { it.type }).containsExactly(CausationType.root)
    }

    // ---- unit of work ---------------------------------------------------------------------------

    private fun eventStore(): EventStore {
        val channel = Grpc.newChannelBuilderForAddress("localhost", 1, InsecureChannelCredentials.create()).build()
        return EventStore("Ordering", "Default", ChronicleServices(channel), ConnectionLifecycle())
    }

    @Test
    fun `unit of work filter puts a unit of work in place for the request`() {
        val store = eventStore()
        var hadOne = false

        UnitOfWorkFilter(store).doFilter(request, response, Observing { hadOne = store.unitOfWorkManager.hasCurrent })

        assertThat(hadOne).isTrue()
    }

    @Test
    fun `unit of work filter leaves no unit of work behind`() {
        val store = eventStore()

        UnitOfWorkFilter(store).doFilter(request, response, MockFilterChain())

        assertThat(store.unitOfWorkManager.hasCurrent).isFalse()
    }

    @Test
    fun `unit of work filter rolls back and rethrows when the request fails`() {
        val store = eventStore()

        assertThatThrownBy {
            UnitOfWorkFilter(store).doFilter(request, response, Observing { throw IllegalStateException("boom") })
        }.isInstanceOf(IllegalStateException::class.java).hasMessage("boom")

        assertThat(store.unitOfWorkManager.hasCurrent).isFalse()
    }

    /** Stands in for the claim-bearing principals Spring Security produces, without the OAuth2 modules. */
    class PrincipalWithClaims(private val claims: Map<String, Any>) {
        fun getClaims(): Map<String, Any> = claims
    }
}
