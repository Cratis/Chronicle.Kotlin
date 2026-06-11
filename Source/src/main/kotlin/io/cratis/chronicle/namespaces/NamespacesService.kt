// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.namespaces

import Cratis.Chronicle.Contracts.CratisChronicleContracts
import Cratis.Chronicle.Contracts.NamespacesGrpcKt

class NamespacesService(
    private val eventStoreName: String,
    private val stub: NamespacesGrpcKt.NamespacesCoroutineStub
) {
    /** Ensures a namespace exists in the event store, creating it if absent. */
    suspend fun ensure(namespaceName: String) {
        val request = CratisChronicleContracts.EnsureNamespace.newBuilder()
            .setEventStore(eventStoreName)
            .setName(namespaceName)
            .build()
        stub.ensure(request)
    }
}
