// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

interface IEventLog : IEventSequence {
    val transactional: ITransactionalEventSequence
    override suspend fun append(eventSourceId: String, event: Any, options: AppendOptions?): AppendResult
    override suspend fun appendMany(eventSourceId: String, events: List<Any>, options: AppendOptions?): List<AppendResult>
    override suspend fun hasEventsFor(eventSourceId: String): Boolean
}
