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
import io.cratis.chronicle.captures.ICapturesService
import io.cratis.chronicle.observation.IFailedPartitions
import io.cratis.chronicle.observation.IReactorsService
import io.cratis.chronicle.observation.IReducersService
import io.cratis.chronicle.projections.IProjectionsService
import io.cratis.chronicle.readModels.IReadModelsService
import io.cratis.chronicle.seeding.IEventSeedingService
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
    val compliance: IComplianceService
    val eventTypes: IEventTypesService
    val namespaces: INamespacesService
    val externalServices: IExternalServicesService
    val jobs: IJobsService
    val eventStoreSubscriptions: IEventStoreSubscriptionsService
    val webhooks: IWebhooksService
    val identities: IIdentityManagerService

    /**
     * The partitions observers are currently failing on, and how to get them moving again.
     *
     * A handler that throws stops the event source it threw on and leaves every other one running,
     * so a stuck partition is easy to miss. This is how an application finds out.
     */
    val failedPartitions: IFailedPartitions

    /**
     * Sources outside Chronicle, pulled in and appended as events.
     *
     * Declare an [io.cratis.chronicle.captures.ICapture] and discovery saves and starts it on
     * connect; reach for this when the declaration is not known at build time.
     */
    val captures: ICapturesService

    /**
     * Gets a non-default [IEventSequence] by its [id].
     *
     * Use [eventLog] for the default event log sequence; use this for any other event sequence.
     *
     * @param id The identifier of the event sequence to get.
     * @return The [IEventSequence] instance.
     */
    fun getEventSequence(id: EventSequenceId): IEventSequence

    /**
     * Registers every artifact the application owns with the kernel — event types, read models,
     * constraints, projections, webhooks, reactors, reducers and seeders — in the order the kernel
     * needs them.
     *
     * Called automatically on every connect unless
     * [io.cratis.chronicle.ChronicleOptions.autoDiscoverAndRegister] is turned off. Calling it
     * yourself is safe at any time: it registers the same artifacts again, and never starts a second
     * set of reactor or reducer observations.
     */
    suspend fun registerAll()

    /**
     * Suspends until automatic registration has finished, so the very next append is guaranteed to hit
     * a kernel that already knows the event types.
     *
     * Returns immediately when [io.cratis.chronicle.ChronicleOptions.autoDiscoverAndRegister] is
     * turned off — there is nothing to wait for.
     */
    suspend fun awaitRegistration()
}
