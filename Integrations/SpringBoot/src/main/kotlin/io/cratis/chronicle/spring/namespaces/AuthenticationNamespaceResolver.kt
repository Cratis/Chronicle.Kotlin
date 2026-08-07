// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring.namespaces

import io.cratis.chronicle.EventStoreNamespaceName
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver
import io.cratis.chronicle.spring.security.Claims

/**
 * Takes the namespace from a claim on the currently authenticated principal — the strongest of the
 * built-in strategies, because the tenant then comes from the token rather than from anything a caller
 * can set.
 *
 * Anonymous requests, and work happening outside a request, fall back to
 * [EventStoreNamespaceName.default].
 *
 * @param claim The claim carrying the namespace.
 */
class AuthenticationNamespaceResolver(private val claim: String) : IEventStoreNamespaceResolver {
    override fun resolve(): String {
        val authentication = Claims.currentAuthentication() ?: return EventStoreNamespaceName.default.value
        return Claims.of(authentication, claim) ?: EventStoreNamespaceName.default.value
    }
}
