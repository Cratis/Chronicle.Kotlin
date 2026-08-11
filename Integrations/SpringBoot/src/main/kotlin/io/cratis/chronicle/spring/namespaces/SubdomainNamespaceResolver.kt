// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring.namespaces

import io.cratis.chronicle.EventStoreNamespaceName
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver

/**
 * Takes the namespace from the subdomain of the current request's host — `acme` in `acme.example.com`
 * — for applications that give every tenant its own hostname.
 *
 * A host with no subdomain, and any work happening outside a request, falls back to
 * [EventStoreNamespaceName.default].
 */
class SubdomainNamespaceResolver : IEventStoreNamespaceResolver {
    override fun resolve(): String {
        val host = currentRequest()?.serverName ?: return EventStoreNamespaceName.default.value
        val parts = host.split('.')
        return if (parts.size > 2) parts.first() else EventStoreNamespaceName.default.value
    }
}
