// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.namespaces

interface INamespacesService {
    /** Ensures a namespace exists in the event store, creating it if absent. */
    suspend fun ensure(namespaceName: String)
}
