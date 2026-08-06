// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import io.cratis.chronicle.compliance.IComplianceService
import io.cratis.chronicle.constraints.IConstraintsService
import io.cratis.chronicle.events.IEventTypesService
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventStoreSubscriptions.IEventStoreSubscriptionsService
import io.cratis.chronicle.externalServices.IExternalServicesService
import io.cratis.chronicle.identities.IIdentityManagerService
import io.cratis.chronicle.jobs.IJobsService
import io.cratis.chronicle.namespaces.INamespacesService
import io.cratis.chronicle.observation.IReactorsService
import io.cratis.chronicle.observation.IReducersService
import io.cratis.chronicle.projections.IProjectionsService
import io.cratis.chronicle.readModels.IReadModelsService
import io.cratis.chronicle.seeding.IEventSeedingService
import io.cratis.chronicle.transactions.UnitOfWorkManager
import io.cratis.chronicle.webhooks.IWebhooksService

interface IEventStore {
    val name: String
    val namespace: String
    val eventLog: IEventLog
    val reactors: IReactorsService
    val reducers: IReducersService
    val projections: IProjectionsService
    val constraints: IConstraintsService
    val seeding: IEventSeedingService
    val readModels: IReadModelsService
    val unitOfWorkManager: UnitOfWorkManager
    val compliance: IComplianceService
    val eventTypes: IEventTypesService
    val namespaces: INamespacesService
    val externalServices: IExternalServicesService
    val jobs: IJobsService
    val eventStoreSubscriptions: IEventStoreSubscriptionsService
    val webhooks: IWebhooksService
    val identities: IIdentityManagerService

    /**
     * Gets a non-default [IEventSequence] by its [id].
     *
     * Use [eventLog] for the default event log sequence; use this for any other event sequence.
     *
     * @param id The identifier of the event sequence to get.
     * @return The [IEventSequence] instance.
     */
    fun getEventSequence(id: EventSequenceId): IEventSequence
}
