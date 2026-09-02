// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import Cratis.Chronicle.Contracts.EventStores.Eventstores
import io.cratis.chronicle.artifacts.ArtifactActivator
import io.cratis.chronicle.artifacts.ArtifactRegistrations
import io.cratis.chronicle.artifacts.IArtifactActivator
import io.cratis.chronicle.artifacts.IClientArtifacts
import io.cratis.chronicle.artifacts.IRegistrationGate
import io.cratis.chronicle.artifacts.KnownClientArtifacts
import io.cratis.chronicle.captures.CapturesService
import io.cratis.chronicle.captures.ICapturesService
import io.cratis.chronicle.compliance.ComplianceService
import io.cratis.chronicle.compliance.IComplianceService
import io.cratis.chronicle.connection.ChronicleServices
import io.cratis.chronicle.diagnostics.ChronicleTraces
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
import io.cratis.chronicle.java.BlockingReactorMethodArgumentResolver
import io.cratis.chronicle.java.BlockingReactorMiddleware
import io.cratis.chronicle.java.asArgumentResolver
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
import io.cratis.chronicle.connection.validateCommandResult
import io.cratis.chronicle.readModels.IReadModelsService
import io.cratis.chronicle.readModels.ReadModelsService
import io.cratis.chronicle.seeding.EventSeedingService
import io.cratis.chronicle.seeding.IEventSeedingService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
 * @param traces Produces the spans the client reports.
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
    private val autoDiscoverAndRegister: Boolean = false,
    private val traces: ChronicleTraces = ChronicleTraces.default
) : IEventStore {

    private val registrations = ArtifactRegistrations(this, artifacts, artifactActivator)
    private val registrationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val generationMutex = Mutex()
    private var generation: RegistrationGeneration? = null

    /**
     * Held shut until the first registration pass is through, so the first append after
     * `getEventStore` cannot outrun the event type registration it depends on.
     */
    private val registrationGate = IRegistrationGate { ensureReady() }

    override val eventLog: IEventLog by lazy {
        EventLog(name, namespace, services.eventSequences, traces, registrationGate)
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
            ReactorMethodArguments(discoveredArgumentResolvers() + ReadModelArgument(readModelsService)),
            traces
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
            when (val instance = artifactActivator.activate(resolverClass)) {
                is IReactorMethodArgumentResolver -> instance
                is BlockingReactorMethodArgumentResolver -> instance.asArgumentResolver()
                else -> throw IllegalStateException(
                    "'${resolverClass.simpleName}' was discovered as a reactor argument resolver but is " +
                        "neither an IReactorMethodArgumentResolver nor a BlockingReactorMethodArgumentResolver"
                )
            }
        }

    override val captures: ICapturesService by lazy {
        CapturesService(name, services.captures)
    }

    override val failedPartitions: IFailedPartitions by lazy {
        FailedPartitions(name, namespace, services.failedPartitions, services.observers)
    }

    override val reducers: IReducersService by lazy {
        ReducersService(name, namespace, lifecycle, services.reducers, defaultSinkTypeId, readModelsService, traces)
    }

    override val projections: IProjectionsService by lazy {
        ProjectionsService(name, services.projections, readModelsService, namespace)
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
            // StateFlow replays the current state, so this one collector covers both a store opened
            // after connection and every later reconnect. The deferred selected for a connection id
            // is also the one awaited by the first append and by explicit registerAll callers.
            registrationScope.launch {
                lifecycle.state.collectLatest { state ->
                    if (state.isConnected) registerAutomatically(state.connectionId) else invalidateGeneration()
                }
            }
        }
    }

    override suspend fun registerAll() {
        val connectionId = lifecycle.connections().first()
        registrationFor(connectionId).await()
    }

    override suspend fun awaitRegistration() {
        if (autoDiscoverAndRegister) awaitRegistrationForCurrentConnection()
    }

    private suspend fun ensureReady() {
        val connectionId = lifecycle.connections().first()
        if (autoDiscoverAndRegister) {
            registrationFor(connectionId).await()
        } else {
            provisioningFor(connectionId).await()
        }
    }

    private suspend fun awaitRegistrationForCurrentConnection() {
        val connectionId = lifecycle.connections().first()
        registrationFor(connectionId).await()
    }

    private suspend fun registerAutomatically(connectionId: String) {
        try {
            registrationFor(connectionId).await()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            System.err.println("[EventStore] Automatic registration of artifacts failed: ${exception.message}")
        }
    }

    private suspend fun registrationFor(connectionId: String): Deferred<Unit> = generationMutex.withLock {
        val current = generationFor(connectionId)
        current.registration ?: registrationScope.async(start = CoroutineStart.LAZY) {
            current.provisioning.await()
            registrations.registerAll()
        }.also {
            current.registration = it
            it.start()
        }
    }

    private suspend fun provisioningFor(connectionId: String): Deferred<Unit> = generationMutex.withLock {
        generationFor(connectionId).provisioning.also { it.start() }
    }

    private fun generationFor(connectionId: String): RegistrationGeneration {
        generation?.takeIf { it.connectionId == connectionId }?.let { return it }
        generation?.cancel()
        return RegistrationGeneration(
            connectionId,
            registrationScope.async(start = CoroutineStart.LAZY) { provision() }
        ).also { generation = it }
    }

    private suspend fun invalidateGeneration() = generationMutex.withLock {
        generation?.cancel()
        generation = null
    }

    private suspend fun provision() {
        val request = Eventstores.EnsureEventStoreRequest.newBuilder()
            .setName(name)
            .build()
        val result = services.eventStores.ensureEventStore(request)
        validateCommandResult(
            "ensure event store '$name'",
            result.authorizationFailureReason,
            result.validationResultsList.map { it.message },
            result.exceptionMessagesList
        )
        namespaces.ensure(namespace)
    }

    private class RegistrationGeneration(
        val connectionId: String,
        val provisioning: Deferred<Unit>
    ) {
        var registration: Deferred<Unit>? = null

        fun cancel() {
            registration?.cancel()
            provisioning.cancel()
        }
    }

    override fun getEventSequence(id: EventSequenceId): IEventSequence =
        // The default event log is special-cased so every lookup shares one appendOperations flow.
        if (id == EventSequenceId.eventLog) {
            eventLog
        } else {
            eventSequences.getOrPut(id) {
                EventSequence(id, name, namespace, services.eventSequences, traces, registrationGate)
            }
        }
}
