// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring.namespaces

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

class NamespaceResolverTests {

    @AfterEach
    fun clearContext() {
        RequestContextHolder.resetRequestAttributes()
        SecurityContextHolder.clearContext()
    }

    private fun onRequest(configure: MockHttpServletRequest.() -> Unit) {
        val request = MockHttpServletRequest().apply(configure)
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }

    @Test
    fun `fixed resolves to the namespace it was given`() {
        assertThat(FixedNamespaceResolver("acme").resolve()).isEqualTo("acme")
    }

    @Test
    fun `http header resolves to the value of the header`() {
        onRequest { addHeader("x-tenant", "acme") }

        assertThat(HttpHeaderNamespaceResolver("x-tenant").resolve()).isEqualTo("acme")
    }

    @Test
    fun `http header falls back to the default namespace when the header is missing`() {
        onRequest { }

        assertThat(HttpHeaderNamespaceResolver("x-tenant").resolve()).isEqualTo("Default")
    }

    @Test
    fun `http header falls back to the default namespace when the header is blank`() {
        onRequest { addHeader("x-tenant", "  ") }

        assertThat(HttpHeaderNamespaceResolver("x-tenant").resolve()).isEqualTo("Default")
    }

    @Test
    fun `http header falls back to the default namespace outside a request`() {
        assertThat(HttpHeaderNamespaceResolver("x-tenant").resolve()).isEqualTo("Default")
    }

    @Test
    fun `subdomain resolves to the first label of the host`() {
        onRequest { serverName = "acme.example.com" }

        assertThat(SubdomainNamespaceResolver().resolve()).isEqualTo("acme")
    }

    @Test
    fun `subdomain falls back to the default namespace for a host with no subdomain`() {
        onRequest { serverName = "example.com" }

        assertThat(SubdomainNamespaceResolver().resolve()).isEqualTo("Default")
    }

    @Test
    fun `subdomain falls back to the default namespace outside a request`() {
        assertThat(SubdomainNamespaceResolver().resolve()).isEqualTo("Default")
    }

    @Test
    fun `authentication resolves to the claim on the principal`() {
        authenticateWith(mapOf("tenant_id" to "acme"))

        assertThat(AuthenticationNamespaceResolver("tenant_id").resolve()).isEqualTo("acme")
    }

    @Test
    fun `authentication falls back to the default namespace when the principal has no such claim`() {
        authenticateWith(mapOf("sub" to "someone"))

        assertThat(AuthenticationNamespaceResolver("tenant_id").resolve()).isEqualTo("Default")
    }

    @Test
    fun `authentication falls back to the default namespace for an anonymous caller`() {
        SecurityContextHolder.getContext().authentication = AnonymousAuthenticationToken(
            "key",
            "anonymous",
            listOf(SimpleGrantedAuthority("ROLE_ANONYMOUS"))
        )

        assertThat(AuthenticationNamespaceResolver("tenant_id").resolve()).isEqualTo("Default")
    }

    @Test
    fun `authentication falls back to the default namespace when nobody is authenticated`() {
        assertThat(AuthenticationNamespaceResolver("tenant_id").resolve()).isEqualTo("Default")
    }

    private fun authenticateWith(claims: Map<String, Any>) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(PrincipalWithClaims(claims), "credentials", emptyList())
    }

    /** Stands in for the claim-bearing principals Spring Security produces, without the OAuth2 modules. */
    class PrincipalWithClaims(private val claims: Map<String, Any>) {
        fun getClaims(): Map<String, Any> = claims
    }
}
