// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import io.cratis.chronicle.compliance.ComplianceService
import io.cratis.chronicle.compliance.IComplianceService
import io.cratis.chronicle.connection.ChronicleServices
import io.cratis.chronicle.constraints.ConstraintsService
import io.cratis.chronicle.constraints.IConstraintsService
import io.cratis.chronicle.events.EventTypesService
import io.cratis.chronicle.events.IEventTypesService
import io.cratis.chronicle.eventSequences.EventLog
import io.cratis.chronicle.eventSequences.EventSequence
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventStoreSubscriptions.EventStoreSubscriptionsService
import io.cratis.chronicle.eventStoreSubscriptions.IEventStoreSubscriptionsService
import io.cratis.chronicle.externalServices.ExternalServicesService
import io.cratis.chronicle.externalServices.IExternalServicesService
import io.cratis.chronicle.identities.IIdentityManagerService
import io.cratis.chronicle.identities.IdentityManagerService
import io.cratis.chronicle.jobs.IJobsService
import io.cratis.chronicle.jobs.JobsService
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.namespaces.INamespacesService
import io.cratis.chronicle.namespaces.NamespacesService
import io.cratis.chronicle.webhooks.IWebhooksService
import io.cratis.chronicle.webhooks.WebhooksService
import io.cratis.chronicle.observation.IReactorsService
import io.cratis.chronicle.observation.IReducersService
import io.cratis.chronicle.observation.ReactorsService
import io.cratis.chronicle.observation.ReducersService
import io.cratis.chronicle.projections.IProjectionsService
import io.cratis.chronicle.projections.ProjectionsService
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.readModels.IReadModelsService
import io.cratis.chronicle.readModels.ReadModelsService
import io.cratis.chronicle.seeding.EventSeedingService
import io.cratis.chronicle.seeding.IEventSeedingService
import io.cratis.chronicle.transactions.UnitOfWorkManager
import java.util.concurrent.ConcurrentHashMap

class EventStore(
    override val name: String,
    override val namespace: String,
    private val services: ChronicleServices,
    private val lifecycle: ConnectionLifecycle,
    private val defaultSinkTypeId: String = io.cratis.chronicle.sinks.WellKnownSinkTypes.MONGODB
) : IEventStore {

    override val unitOfWorkManager: UnitOfWorkManager = UnitOfWorkManager()

    override val eventLog: IEventLog by lazy {
        EventLog(name, namespace, services.eventSequences, unitOfWorkManager)
    }

    // ReadModelsService is shared so that reducers and projections can auto-register their read
    // models with the correct observer type without the caller having to set it on @ReadModel.
    private val readModelsService: ReadModelsService by lazy {
        ReadModelsService(name, namespace, services.readModels, services.materializedReadModels, services.compliance, defaultSinkTypeId)
    }

    override val readModels: IReadModelsService get() = readModelsService

    override val reactors: IReactorsService by lazy {
        ReactorsService(name, namespace, lifecycle, services.reactors, eventLog)
    }

    override val reducers: IReducersService by lazy {
        ReducersService(name, namespace, lifecycle, services.reducers, defaultSinkTypeId, readModelsService)
    }

    override val projections: IProjectionsService by lazy {
        ProjectionsService(name, services.projections, readModelsService)
    }

    override val constraints: IConstraintsService by lazy {
        ConstraintsService(name, services.constraints)
    }

    override val seeding: IEventSeedingService by lazy {
        EventSeedingService(name, namespace, services.eventSeeding)
    }

    override val compliance: IComplianceService by lazy {
        ComplianceService(name, namespace, services.compliance)
    }

    override val eventTypes: IEventTypesService by lazy {
        EventTypesService(name, services.eventTypes)
    }

    override val namespaces: INamespacesService by lazy {
        NamespacesService(name, services.namespaces)
    }

    override val externalServices: IExternalServicesService by lazy {
        ExternalServicesService(name, services.externalServices)
    }

    override val jobs: IJobsService by lazy {
        JobsService(name, namespace, services.jobs)
    }

    override val eventStoreSubscriptions: IEventStoreSubscriptionsService by lazy {
        EventStoreSubscriptionsService(name, services.eventStoreSubscriptions)
    }

    override val webhooks: IWebhooksService by lazy {
        WebhooksService(name, services.webhooks)
    }

    override val identities: IIdentityManagerService by lazy {
        IdentityManagerService(name, namespace, services.identities)
    }

    private val eventSequences = ConcurrentHashMap<EventSequenceId, IEventSequence>()

    override fun getEventSequence(id: EventSequenceId): IEventSequence =
        eventSequences.getOrPut(id) {
            EventSequence(id, name, namespace, services.eventSequences)
        }
}
