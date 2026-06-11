// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import Cratis.Chronicle.Contracts.Clients.ConnectionServiceGrpcKt
import Cratis.Chronicle.Contracts.Compliance.ComplianceGrpcKt
import Cratis.Chronicle.Contracts.Events.Constraints.ConstraintsGrpcKt
import Cratis.Chronicle.Contracts.Events.EventTypesGrpcKt
import Cratis.Chronicle.Contracts.EventSequences.EventSequencesGrpcKt
import Cratis.Chronicle.Contracts.NamespacesGrpcKt
import Cratis.Chronicle.Contracts.Observation.Reactors.ReactorsGrpcKt
import Cratis.Chronicle.Contracts.Observation.Reducers.ReducersGrpcKt
import Cratis.Chronicle.Contracts.Projections.ProjectionsGrpcKt
import Cratis.Chronicle.Contracts.ReadModels.ReadModelsGrpcKt
import Cratis.Chronicle.Contracts.Seeding.EventSeedingGrpcKt

class ChronicleServices(channel: io.grpc.ManagedChannel) {
    val connection: ConnectionServiceGrpcKt.ConnectionServiceCoroutineStub =
        ConnectionServiceGrpcKt.ConnectionServiceCoroutineStub(channel)
    val eventSequences: EventSequencesGrpcKt.EventSequencesCoroutineStub =
        EventSequencesGrpcKt.EventSequencesCoroutineStub(channel)
    val reactors: ReactorsGrpcKt.ReactorsCoroutineStub =
        ReactorsGrpcKt.ReactorsCoroutineStub(channel)
    val reducers: ReducersGrpcKt.ReducersCoroutineStub =
        ReducersGrpcKt.ReducersCoroutineStub(channel)
    val projections: ProjectionsGrpcKt.ProjectionsCoroutineStub =
        ProjectionsGrpcKt.ProjectionsCoroutineStub(channel)
    val constraints: ConstraintsGrpcKt.ConstraintsCoroutineStub =
        ConstraintsGrpcKt.ConstraintsCoroutineStub(channel)
    val eventSeeding: EventSeedingGrpcKt.EventSeedingCoroutineStub =
        EventSeedingGrpcKt.EventSeedingCoroutineStub(channel)
    val readModels: ReadModelsGrpcKt.ReadModelsCoroutineStub =
        ReadModelsGrpcKt.ReadModelsCoroutineStub(channel)
    val compliance: ComplianceGrpcKt.ComplianceCoroutineStub =
        ComplianceGrpcKt.ComplianceCoroutineStub(channel)
    val eventTypes: EventTypesGrpcKt.EventTypesCoroutineStub =
        EventTypesGrpcKt.EventTypesCoroutineStub(channel)
    val namespaces: NamespacesGrpcKt.NamespacesCoroutineStub =
        NamespacesGrpcKt.NamespacesCoroutineStub(channel)
}
