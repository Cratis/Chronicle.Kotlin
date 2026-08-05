// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import io.cratis.chronicle.transactions.IUnitOfWorkManager

class TransactionalEventSequence(
    private val inner: EventSequence,
    private val unitOfWorkManager: IUnitOfWorkManager
) : ITransactionalEventSequence {

    override suspend fun append(eventSourceId: String, event: Any, options: AppendOptions?): AppendResult {
        if (unitOfWorkManager.hasCurrent) {
            unitOfWorkManager.current.addEvent(inner.id, eventSourceId, event, options)
            return AppendResult(EventSequenceNumber(-1), emptyList(), emptyList(), true)
        }
        return inner.append(eventSourceId, event, options)
    }

    override suspend fun appendMany(eventSourceId: String, events: List<Any>, options: AppendOptions?): List<AppendResult> {
        if (unitOfWorkManager.hasCurrent) {
            val uow = unitOfWorkManager.current
            events.forEach { uow.addEvent(inner.id, eventSourceId, it, options) }
            return events.map { AppendResult(EventSequenceNumber(-1), emptyList(), emptyList(), true) }
        }
        return inner.appendMany(eventSourceId, events, options)
    }
}
