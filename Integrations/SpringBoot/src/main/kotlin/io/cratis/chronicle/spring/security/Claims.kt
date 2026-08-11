// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring.security

import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Reads claims off whatever the current principal happens to be.
 *
 * Spring Security models an authenticated principal a dozen different ways — a `Jwt`, an `OidcUser`, a
 * `DefaultOAuth2User`, a plain `UserDetails` — and the claim-bearing ones expose their claims through
 * either `getClaims()` or `getAttributes()`. Reading those two by reflection means the starter supports
 * all of them without dragging in the OAuth2 and JOSE modules, which most applications using a plain
 * `UserDetailsService` would never otherwise need.
 */
internal object Claims {
    private val CLAIM_ACCESSORS = arrayOf("getClaims", "getAttributes")

    /** The currently authenticated principal, or `null` when nobody is authenticated. */
    fun currentAuthentication(): Authentication? =
        SecurityContextHolder.getContext()?.authentication
            ?.takeIf { it.isAuthenticated && it !is AnonymousAuthenticationToken }

    /**
     * The value of [name] on the current principal.
     *
     * @param authentication The authentication to read from.
     * @param name The claim to read.
     * @return The claim value, or `null` when the principal carries no such claim.
     */
    fun of(authentication: Authentication, name: String): String? =
        claims(authentication.principal)?.get(name)?.toString()?.takeIf { it.isNotBlank() }

    private fun claims(principal: Any?): Map<*, *>? {
        if (principal == null) return null
        for (accessor in CLAIM_ACCESSORS) {
            val claims = runCatching {
                principal.javaClass.getMethod(accessor).invoke(principal) as? Map<*, *>
            }.getOrNull()
            if (claims != null) return claims
        }
        return null
    }
}
