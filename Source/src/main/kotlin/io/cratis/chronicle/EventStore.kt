// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import io.cratis.chronicle.artifacts.ArtifactActivator
import io.cratis.chronicle.artifacts.ArtifactRegistrations
import io.cratis.chronicle.artifacts.IArtifactActivator
import io.cratis.chronicle.artifacts.IClientArtifacts
import io.cratis.chronicle.artifacts.KnownClientArtifacts
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
import io.cratis.chronicle.java.BlockingReactorMiddleware
import io.cratis.chronicle.java.asReactorMiddleware
import io.cratis.chronicle.observation.FailedPartitions
import io.cratis.chronicle.observation.IFailedPartitions
import io.cratis.chronicle.observation.IReactorMethodArgumentResolver
import io.cratis.chronicle.observation.IReactorMiddleware
import io.cratis.chronicle.observation.IReactorsService
import io.cratis.chronicle.observation.IReducersService
import io.cratis.chronicle.observation.ReactorMethodArguments
import io.cratis.chronicle.observation.ReactorMiddlewares
import io.cratis.chronicle.observation.ReactorsService
import io.cratis.chronicle.observation.ReadModelArgument
import io.cratis.chronicle.observation.ReducersService
import io.cratis.chronicle.projections.IProjectionsService
import io.cratis.chronicle.projections.ProjectionsService
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.readModels.IReadModelsService
import io.cratis.chronicle.readModels.ReadModelsService
import io.cratis.chronicle.seeding.EventSeedingService
import io.cratis.chronicle.seeding.IEventSeedingService
import io.cratis.chronicle.transactions.UnitOfWorkManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * An event store, and everything you can do with it.
 *
 * @param name The name of the event store.
 * @param namespace The namespace within the event store.
 * @param services The gRPC stubs to talk to the kernel through.
 * @param lifecycle Tracks whether the client is connected, and under which connection id.
 * @param defaultSinkTypeId The sink read models are persisted to by default.
 * @param artifacts What the application consists of. Only consulted when [autoDiscoverAndRegister] is on.
 * @param artifactActivator Creates the instances for discovered artifacts.
 * @param autoDiscoverAndRegister Whether to register every artifact automatically on connect. Off by
 *   default here so that constructing an event store directly never reaches out to the kernel on its
 *   own; [ChronicleClient] turns it on from [ChronicleOptions.autoDiscoverAndRegister].
 */
class EventStore(
    override val name: String,
    override val namespace: String,
    private val services: ChronicleServices,
    private val lifecycle: ConnectionLifecycle,
    private val defaultSinkTypeId: String = io.cratis.chronicle.sinks.WellKnownSinkTypes.MONGODB,
    private val artifacts: IClientArtifacts = KnownClientArtifacts.empty,
    private val artifactActivator: IArtifactActivator = ArtifactActivator,
    private val autoDiscoverAndRegister: Boolean = false
) : IEventStore {

    private val registrations = ArtifactRegistrations(this, artifacts, artifactActivator)

    override val unitOfWorkManager: UnitOfWorkManager = UnitOfWorkManager(this)

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
        ReactorsService(
            name,
            namespace,
            lifecycle,
            services.reactors,
            eventLog,
            ReactorMiddlewares(discoveredMiddlewares()),
            ReactorMethodArguments(discoveredArgumentResolvers() + ReadModelArgument(readModelsService))
        )
    }

    /**
     * The middlewares wrapped around every reactor handler, in the order they were discovered.
     *
     * Unlike the artifacts declared to the kernel, these are created here rather than by
     * [ArtifactRegistrations] - they are never registered anywhere, they simply take part in
     * dispatch, and the reactors service needs them before the first event arrives.
     */
    private fun discoveredMiddlewares(): List<IReactorMiddleware> =
        artifacts.reactorMiddlewares.map { middlewareClass ->
            when (val instance = artifactActivator.activate(middlewareClass)) {
                is IReactorMiddleware -> instance
                is BlockingReactorMiddleware -> instance.asReactorMiddleware()
                else -> throw IllegalStateException(
                    "'${middlewareClass.simpleName}' was discovered as a reactor middleware but is neither " +
                        "an IReactorMiddleware nor a BlockingReactorMiddleware"
                )
            }
        }

    /**
     * The resolvers consulted for handler parameters past the event.
     *
     * Discovered ones come first so an application can take over a parameter the built-in read model
     * resolver would otherwise claim.
     */
    private fun discoveredArgumentResolvers(): List<IReactorMethodArgumentResolver> =
        artifacts.reactorArgumentResolvers.map { resolverClass ->
            artifactActivator.activate(resolverClass) as IReactorMethodArgumentResolver
        }

    override val failedPartitions: IFailedPartitions by lazy {
        FailedPartitions(name, namespace, services.failedPartitions, services.observers)
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
        EventStoreSubscriptionsService(name, services.eventStoreSubscriptions, eventTypes)
    }

    override val webhooks: IWebhooksService by lazy {
        WebhooksService(name, services.webhooks)
    }

    override val identities: IIdentityManagerService by lazy {
        IdentityManagerService(name, namespace, services.identities)
    }

    private val eventSequences = ConcurrentHashMap<EventSequenceId, IEventSequence>()

    init {
        if (autoDiscoverAndRegister) {
            // Registration is redone on every connection, not just the first. A kernel that restarted
            // has forgotten every declaration made to it, and the client is expected to state them again.
            CoroutineScope(Dispatchers.IO).launch {
                lifecycle.connections().collect {
                    try {
                        registrations.registerAll()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        System.err.println("[EventStore] Automatic registration of artifacts failed: ${e.message}")
                    }
                }
            }
        }
    }

    override suspend fun registerAll() {
        registrations.registerAll()
    }

    override suspend fun awaitRegistration() {
        if (autoDiscoverAndRegister) registrations.completed.await()
    }

    override fun getEventSequence(id: EventSequenceId): IEventSequence =
        // The default event log is special-cased so that anything resolving it by id - such as a
        // UnitOfWork committing staged events - gets the same instance as `eventLog`, sharing its
        // appendOperations flow and transactional wiring rather than a disconnected duplicate.
        if (id == EventSequenceId.eventLog) {
            eventLog
        } else {
            eventSequences.getOrPut(id) {
                EventSequence(id, name, namespace, services.eventSequences)
            }
        }
}
