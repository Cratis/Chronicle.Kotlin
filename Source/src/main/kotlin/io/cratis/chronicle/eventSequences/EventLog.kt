// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.EventSequences.EventSequencesGrpcKt
import io.cratis.chronicle.transactions.UnitOfWorkManager

class EventLog(
    name: String,
    namespace: String,
    stub: EventSequencesGrpcKt.EventSequencesCoroutineStub,
    private val unitOfWorkManager: UnitOfWorkManager = UnitOfWorkManager()
) : EventSequence(EventSequenceId.eventLog, name, namespace, stub), IEventLog {

    override val transactional: ITransactionalEventSequence by lazy {
        TransactionalEventSequence(this, unitOfWorkManager)
    }
}
