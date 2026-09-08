// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring

import io.cratis.chronicle.IChronicleClient
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.captures.ICapturesService
import io.cratis.chronicle.compliance.IComplianceService
import io.cratis.chronicle.constraints.IConstraintsService
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventStoreSubscriptions.IEventStoreSubscriptionsService
import io.cratis.chronicle.events.IEventTypesService
import io.cratis.chronicle.externalServices.IExternalServicesService
import io.cratis.chronicle.identities.IIdentityManagerService
import io.cratis.chronicle.jobs.IJobsService
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver
import io.cratis.chronicle.namespaces.INamespacesService
import io.cratis.chronicle.observation.IFailedPartitions
import io.cratis.chronicle.observation.IReactorsService
import io.cratis.chronicle.observation.IReducersService
import io.cratis.chronicle.projections.IProjectionsService
import io.cratis.chronicle.readModels.IReadModelsService
import io.cratis.chronicle.seeding.IEventSeedingService
import io.cratis.chronicle.webhooks.IWebhooksService

/**
 * The event store for the work currently being done, whichever namespace that turns out to be.
 *
 * This is what gets injected wherever an application asks for an `IEventStore`. It is a singleton, but
 * it holds no state of its own: every call asks the [IEventStoreNamespaceResolver] which namespace the
 * current piece of work belongs to and forwards to the event store for it. A single-tenant application
 * therefore always talks to the same store, while a multi-tenant one is routed per request without a
 * single line of tenant plumbing in application code.
 *
 * The client caches an event store per name and namespace, so resolution costs a map lookup.
 *
 * @param client The client to get event stores from.
 * @param eventStoreName The event store this application works against.
 * @param namespaceResolver Decides which namespace the current piece of work belongs to.
 */
class ResolvedEventStore(
    private val client: IChronicleClient,
    private val eventStoreName: String,
    private val namespaceResolver: IEventStoreNamespaceResolver
) : IEventStore {
    /** The event store for the namespace the current piece of work belongs to. */
    val current: IEventStore get() = client.getEventStore(eventStoreName, namespaceResolver.resolve())

    override val name: String get() = current.name
    override val namespace: String get() = current.namespace
    override val eventLog: IEventLog get() = current.eventLog
    override val reactors: IReactorsService get() = current.reactors
    override val reducers: IReducersService get() = current.reducers
    override val projections: IProjectionsService get() = current.projections
    override val constraints: IConstraintsService get() = current.constraints
    override val seeding: IEventSeedingService get() = current.seeding
    override val readModels: IReadModelsService get() = current.readModels
    override val compliance: IComplianceService get() = current.compliance
    override val eventTypes: IEventTypesService get() = current.eventTypes
    override val namespaces: INamespacesService get() = current.namespaces
    override val externalServices: IExternalServicesService get() = current.externalServices
    override val jobs: IJobsService get() = current.jobs
    override val eventStoreSubscriptions: IEventStoreSubscriptionsService get() = current.eventStoreSubscriptions
    override val webhooks: IWebhooksService get() = current.webhooks
    override val identities: IIdentityManagerService get() = current.identities
    override val failedPartitions: IFailedPartitions get() = current.failedPartitions
    override val captures: ICapturesService get() = current.captures

    override fun getEventSequence(id: EventSequenceId): IEventSequence = current.getEventSequence(id)

    override suspend fun registerAll() = current.registerAll()

    override suspend fun awaitRegistration() = current.awaitRegistration()
}
