// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import io.cratis.chronicle.compliance.ComplianceService
import io.cratis.chronicle.connection.ChronicleServices
import io.cratis.chronicle.constraints.ConstraintsService
import io.cratis.chronicle.constraints.IConstraintsService
import io.cratis.chronicle.events.EventTypesService
import io.cratis.chronicle.eventSequences.EventLog
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.namespaces.NamespacesService
import io.cratis.chronicle.observation.IReactorsService
import io.cratis.chronicle.observation.IReducersService
import io.cratis.chronicle.observation.ReactorsService
import io.cratis.chronicle.observation.ReducersService
import io.cratis.chronicle.projections.IProjectionsService
import io.cratis.chronicle.projections.ProjectionsService
import io.cratis.chronicle.readModels.IReadModelsService
import io.cratis.chronicle.readModels.ReadModelsService
import io.cratis.chronicle.seeding.EventSeedingService
import io.cratis.chronicle.seeding.IEventSeedingService
import io.cratis.chronicle.transactions.UnitOfWorkManager

class EventStore(
    override val name: String,
    override val namespace: String,
    private val services: ChronicleServices,
    private val connectionId: String,
    private val defaultSinkTypeId: String = io.cratis.chronicle.sinks.WellKnownSinkTypes.MONGODB
) : IEventStore {

    override val unitOfWorkManager: UnitOfWorkManager = UnitOfWorkManager()

    override val eventLog: IEventLog by lazy {
        EventLog(name, namespace, services.eventSequences, unitOfWorkManager)
    }

    override val reactors: IReactorsService by lazy {
        ReactorsService(name, namespace, connectionId, services.reactors)
    }

    override val reducers: IReducersService by lazy {
        ReducersService(name, namespace, connectionId, services.reducers, defaultSinkTypeId)
    }

    override val projections: IProjectionsService by lazy {
        ProjectionsService(name, services.projections)
    }

    override val constraints: IConstraintsService by lazy {
        ConstraintsService(name, services.constraints)
    }

    override val seeding: IEventSeedingService by lazy {
        EventSeedingService(name, namespace, services.eventSeeding)
    }

    override val readModels: IReadModelsService by lazy {
        ReadModelsService(name, namespace, services.readModels)
    }

    val compliance by lazy {
        ComplianceService(name, namespace, services.compliance)
    }

    val eventTypes by lazy {
        EventTypesService(name, services.eventTypes)
    }

    val namespaces by lazy {
        NamespacesService(name, services.namespaces)
    }
}
