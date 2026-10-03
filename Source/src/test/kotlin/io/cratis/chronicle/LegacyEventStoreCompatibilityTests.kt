// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import Cratis.Chronicle.Contracts.Sequences.EventSequencesGrpcKt
import Cratis.Chronicle.Contracts.Sequences.Sequences
import io.cratis.chronicle.artifacts.ArtifactRegistrations
import io.cratis.chronicle.artifacts.IRegistrationGate
import io.cratis.chronicle.artifacts.KnownClientArtifacts
import io.cratis.chronicle.compliance.IComplianceService
import io.cratis.chronicle.captures.ICapturesService
import io.cratis.chronicle.constraints.IConstraintsService
import io.cratis.chronicle.diagnostics.ChronicleTraces
import io.cratis.chronicle.eventSequences.EventLog
import io.cratis.chronicle.eventSequences.EventSequence
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSources.EventSource
import io.cratis.chronicle.eventSources.IEventSources
import io.cratis.chronicle.eventSources.IEventSourcesCapability
import io.cratis.chronicle.events.IEventTypesService
import io.cratis.chronicle.eventStoreSubscriptions.IEventStoreSubscriptionsService
import io.cratis.chronicle.externalServices.IExternalServicesService
import io.cratis.chronicle.identities.IIdentityManagerService
import io.cratis.chronicle.jobs.IJobsService
import io.cratis.chronicle.namespaces.INamespacesService
import io.cratis.chronicle.observation.IFailedPartitions
import io.cratis.chronicle.observation.IObservers
import io.cratis.chronicle.observation.IReactorsService
import io.cratis.chronicle.observation.IReducersService
import io.cratis.chronicle.projections.IProjectionsService
import io.cratis.chronicle.readModels.IReadModelsService
import io.cratis.chronicle.seeding.IEventSeedingService
import io.cratis.chronicle.transactions.UnitOfWorkManager
import io.cratis.chronicle.webhooks.IWebhooksService
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private data class LegacyHappened(val value: Int)

@EventSource("Legacy")
private class LegacyEventSource

/**
 * A hand-written event store that implements only the members `IEventStore` had before event sources
 * existed. It must keep compiling and behaving as it did.
 */
private class LegacyEventStore(override val eventLog: IEventLog, override val eventTypes: IEventTypesService) : IEventStore {
    override val name = "legacy"
    override val namespace = "default"
    override val reactors: IReactorsService = mockk(relaxed = true)
    override val reducers: IReducersService = mockk(relaxed = true)
    override val projections: IProjectionsService = mockk(relaxed = true)
    override val constraints: IConstraintsService = mockk(relaxed = true)
    override val seeding: IEventSeedingService = mockk(relaxed = true)
    override val readModels: IReadModelsService = mockk(relaxed = true)
    override val unitOfWorkManager: UnitOfWorkManager = mockk(relaxed = true)
    override val compliance: IComplianceService = mockk(relaxed = true)
    override val namespaces: INamespacesService = mockk(relaxed = true)
    override val externalServices: IExternalServicesService = mockk(relaxed = true)
    override val jobs: IJobsService = mockk(relaxed = true)
    override val eventStoreSubscriptions: IEventStoreSubscriptionsService = mockk(relaxed = true)
    override val webhooks: IWebhooksService = mockk(relaxed = true)
    override val identities: IIdentityManagerService = mockk(relaxed = true)
    override val failedPartitions: IFailedPartitions = mockk(relaxed = true)
    override val observers: IObservers = mockk(relaxed = true)
    override val captures: ICapturesService = mockk(relaxed = true)

    override fun getEventSequence(id: EventSequenceId): IEventSequence = eventLog

    override suspend fun registerAll() = Unit

    override suspend fun awaitRegistration() = Unit
}

class LegacyEventStoreCompatibilityTests {
    private val stub = mockk<EventSequencesGrpcKt.EventSequencesCoroutineStub>()
    private val append = slot<Sequences.AppendRequest>()

    private fun legacyStore(): LegacyEventStore {
        coEvery { stub.append(capture(append), any()) } returns Sequences.CommandResult_AppendResponse.newBuilder()
            .setIsAuthorized(true).setResponse(Sequences.AppendResponse.newBuilder().setSequenceNumber(0)).build()
        // The pre-event-sources EventLog constructor.
        val log = EventLog("legacy", "default", stub, mockk(relaxed = true), ChronicleTraces.default, IRegistrationGate.open)
        return LegacyEventStore(log, mockk(relaxed = true))
    }

    @Test
    fun `a store written against the old interface reports event sources as unsupported`() {
        val store = legacyStore()

        assertFalse((store as IEventStore) is IEventSourcesCapability)
        assertSame(IEventSources.unsupported, store.eventSources)
        assertTrue(store.eventSources.all.isEmpty())
    }

    @Test
    fun `an unsupported store fails loudly when a definition is asked for`() {
        val store = legacyStore()

        assertThrows<UnsupportedOperationException> { store.eventSources.getFor(LegacyEventSource::class) }
        assertThrows<UnsupportedOperationException> { store.eventSources.getFor("Legacy") }
    }

    @Test
    fun `an old style append through a legacy store carries no event source`() = runBlocking {
        val store = legacyStore()

        val result = store.eventLog.append("source-1", LegacyHappened(1))

        assertTrue(result.isSuccess)
        assertEquals("", append.captured.eventSource)
        assertEquals("source-1", append.captured.eventSourceId)
    }

    @Test
    fun `old style event sequences without a registry still append without routing`() = runBlocking {
        legacyStore()
        val sequence = EventSequence(
            EventSequenceId.eventLog, "legacy", "default", stub, ChronicleTraces.default,
            IRegistrationGate.open
        )

        sequence.append("source-1", LegacyHappened(1))

        assertEquals("", append.captured.eventSource)
    }

    @Test
    fun `registration against a legacy store without declared event sources is unaffected`() = runBlocking {
        val store = legacyStore()

        ArtifactRegistrations(store, KnownClientArtifacts()).registerAll()
    }

    @Test
    fun `declared event sources on a legacy store fail registration rather than being dropped`() {
        val store = legacyStore()
        val artifacts = KnownClientArtifacts(LegacyEventSource::class)

        assertThrows<IllegalStateException> { runBlocking { ArtifactRegistrations(store, artifacts).registerAll() } }
    }
}
