// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import io.cratis.chronicle.transactions.UnitOfWorkManager

class TransactionalEventSequence(
    private val inner: EventSequence,
    private val unitOfWorkManager: UnitOfWorkManager
) : ITransactionalEventSequence {

    override suspend fun append(eventSourceId: String, event: Any, options: AppendOptions?): AppendResult {
        val uow = unitOfWorkManager.current
        return if (uow != null) {
            uow.stage(eventSourceId, event)
            AppendResult(EventSequenceNumber(-1), emptyList(), emptyList(), true)
        } else {
            inner.append(eventSourceId, event, options)
        }
    }

    override suspend fun appendMany(eventSourceId: String, events: List<Any>, options: AppendOptions?): List<AppendResult> {
        val uow = unitOfWorkManager.current
        return if (uow != null) {
            events.forEach { uow.stage(eventSourceId, it) }
            events.map { AppendResult(EventSequenceNumber(-1), emptyList(), emptyList(), true) }
        } else {
            inner.appendMany(eventSourceId, events, options)
        }
    }
}
