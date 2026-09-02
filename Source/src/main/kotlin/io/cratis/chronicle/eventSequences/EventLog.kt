// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSequences

import Cratis.Chronicle.Contracts.EventSequences.EventSequencesGrpcKt
import io.cratis.chronicle.artifacts.IRegistrationGate
import io.cratis.chronicle.diagnostics.ChronicleTraces

/** Implements the default event-log sequence. */
class EventLog(
    name: String,
    namespace: String,
    stub: EventSequencesGrpcKt.EventSequencesCoroutineStub,
    traces: ChronicleTraces = ChronicleTraces.default,
    registrationGate: IRegistrationGate = IRegistrationGate.open
) : EventSequence(EventSequenceId.eventLog, name, namespace, stub, traces, registrationGate), IEventLog
