// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.namespaces

import Cratis.Chronicle.Contracts.Namespaces.NamespacesGrpcKt
import Cratis.Chronicle.Contracts.Namespaces.NamespacesOuterClass
import kotlinx.coroutines.flow.first

class NamespacesService(
    private val eventStoreName: String,
    private val stub: NamespacesGrpcKt.NamespacesCoroutineStub
) : INamespacesService {
    /** Ensures a namespace exists in the event store, creating it if absent. */
    override suspend fun ensure(namespaceName: String) {
        val request = NamespacesOuterClass.EnsureNamespaceRequest.newBuilder()
            .setEventStore(eventStoreName)
            .setNamespace(namespaceName)
            .build()
        stub.ensureNamespace(request)
    }

    /**
     * Lists all namespaces in the event store.
     *
     * The kernel serves this as an observable query - a stream that emits the whole list again
     * every time it changes - so this takes the first emission and lets the subscription go.
     */
    override suspend fun getAll(): List<String> {
        val request = NamespacesOuterClass.AllNamespacesRequest.newBuilder()
            .setEventStore(eventStoreName)
            .build()
        return stub.allNamespaces(request).first().dataList
    }
}
