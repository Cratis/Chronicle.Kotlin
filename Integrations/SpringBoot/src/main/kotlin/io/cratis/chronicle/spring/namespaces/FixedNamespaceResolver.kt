// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring.namespaces

import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver

/**
 * Puts everything in one namespace — the single-tenant default.
 *
 * @param namespace The namespace every piece of work belongs to.
 */
class FixedNamespaceResolver(private val namespace: String) : IEventStoreNamespaceResolver {
    override fun resolve(): String = namespace
}
