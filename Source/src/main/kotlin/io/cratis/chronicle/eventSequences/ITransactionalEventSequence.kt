// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

interface ITransactionalEventSequence {
    suspend fun append(eventSourceId: String, event: Any, options: AppendOptions? = null): AppendResult
    suspend fun appendMany(eventSourceId: String, events: List<Any>, options: AppendOptions? = null): List<AppendResult>
}
