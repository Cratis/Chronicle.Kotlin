// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.namespaces

import io.cratis.chronicle.EventStoreNamespaceName

/**
 * The [IEventStoreNamespaceResolver] used by single-tenant applications: everything lives in
 * [EventStoreNamespaceName.default].
 */
object DefaultEventStoreNamespaceResolver : IEventStoreNamespaceResolver {
    override fun resolve(): String = EventStoreNamespaceName.default.value
}
