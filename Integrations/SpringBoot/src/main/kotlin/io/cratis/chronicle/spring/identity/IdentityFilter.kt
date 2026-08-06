// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring.identity

import io.cratis.chronicle.identity.Identity
import io.cratis.chronicle.identity.identityProvider
import io.cratis.chronicle.spring.security.Claims
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Makes the authenticated user the identity recorded on every event appended while handling a request.
 *
 * Chronicle stores who caused each event alongside the event itself. Setting that from the security
 * context here means application code never passes a user around just to get it onto an event — the
 * audit trail comes out right by default.
 *
 * Runs after Spring Security's filter chain so the principal is already established, and clears the
 * identity afterwards so a pooled request thread never inherits the previous request's user.
 */
class IdentityFilter : OncePerRequestFilter(), Ordered {
    override fun getOrder(): Int = ORDER

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authentication = Claims.currentAuthentication()
        if (authentication != null) {
            identityProvider.setCurrentIdentity(
                Identity(
                    subject = Claims.of(authentication, SUBJECT_CLAIM) ?: authentication.name.orEmpty(),
                    name = Claims.of(authentication, NAME_CLAIM) ?: authentication.name.orEmpty(),
                    userName = Claims.of(authentication, USERNAME_CLAIM) ?: authentication.name.orEmpty()
                )
            )
        }

        try {
            filterChain.doFilter(request, response)
        } finally {
            identityProvider.clearCurrentIdentity()
        }
    }

    private companion object {
        /** Late enough that Spring Security has authenticated the request, early enough to cover every handler. */
        const val ORDER = Ordered.LOWEST_PRECEDENCE - 30

        const val SUBJECT_CLAIM = "sub"
        const val NAME_CLAIM = "name"
        const val USERNAME_CLAIM = "preferred_username"
    }
}
