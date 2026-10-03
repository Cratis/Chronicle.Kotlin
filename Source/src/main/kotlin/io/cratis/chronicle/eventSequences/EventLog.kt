// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.Sequences.EventSequencesGrpcKt
import io.cratis.chronicle.artifacts.IRegistrationGate
import io.cratis.chronicle.diagnostics.ChronicleTraces
import io.cratis.chronicle.eventSources.IEventSources
import io.cratis.chronicle.transactions.IUnitOfWorkManager

class EventLog(
    name: String,
    namespace: String,
    stub: EventSequencesGrpcKt.EventSequencesCoroutineStub,
    private val unitOfWorkManager: IUnitOfWorkManager,
    traces: ChronicleTraces = ChronicleTraces.default,
    registrationGate: IRegistrationGate = IRegistrationGate.open,
    eventSources: IEventSources = IEventSources.none
) : EventSequence(EventSequenceId.eventLog, name, namespace, stub, traces, registrationGate, eventSources), IEventLog {
    /** The shape from before event sources existed, with its original default arguments. */
    constructor(
        name: String,
        namespace: String,
        stub: EventSequencesGrpcKt.EventSequencesCoroutineStub,
        unitOfWorkManager: IUnitOfWorkManager,
        traces: ChronicleTraces = ChronicleTraces.default,
        registrationGate: IRegistrationGate = IRegistrationGate.open
    ) : this(name, namespace, stub, unitOfWorkManager, traces, registrationGate, IEventSources.none)

    override val transactional: ITransactionalEventSequence by lazy {
        TransactionalEventSequence(this, unitOfWorkManager)
    }
}
