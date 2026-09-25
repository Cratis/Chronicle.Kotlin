// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import Cratis.Chronicle.Contracts.Compliance.ComplianceGrpcKt
import Cratis.Chronicle.Contracts.Compliance.ComplianceOuterClass
import Cratis.Chronicle.Contracts.ReadModelExplorer.ReadModelExplorerGrpcKt
import Cratis.Chronicle.Contracts.ReadModelExplorer.Readmodelexplorer
import Cratis.Chronicle.Contracts.ReadModels.MaterializedReadModelsGrpcKt
import Cratis.Chronicle.Contracts.ReadModels.ReadModelsGrpcKt
import Cratis.Chronicle.Contracts.ReadModels.Readmodels
import bcl.Bcl
import com.google.protobuf.Empty
import io.cratis.chronicle.Subject
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.eventSequences.AppendedEvent
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.events.EventTypeDescriptor
import io.cratis.chronicle.events.EventTypeGeneration
import io.cratis.chronicle.events.EventTypeId
import io.cratis.chronicle.identity.Identity
import io.cratis.chronicle.observation.ReducersService
import io.cratis.chronicle.observation.EventSequence
import io.cratis.chronicle.observation.EventSourceType
import io.cratis.chronicle.observation.EventStreamType
import io.cratis.chronicle.observation.FilterEventsByTag
import io.cratis.chronicle.sinks.WellKnownSinkTypes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private data class EmployeeState(val name: String, val title: String)

@Passive
private data class PassiveEmployee(val name: String = "")

@EventType
private data class EmployeeHired(val name: String)

@EventType
private data class EmployeePromoted(val title: String)

@EventType
private data class EmployeeRenamed(val name: String)

@io.cratis.chronicle.observation.Reducer(isActive = false)
private class PassiveEmployeeReducer {
    fun hired(event: EmployeeHired) = EmployeeState(event.name, "New")
    fun promoted(event: EmployeePromoted, state: EmployeeState?) = EmployeeState(state!!.name, event.title)
    suspend fun renamed(event: EmployeeRenamed, state: EmployeeState?, context: EventContext) =
        EmployeeState(event.name, "${state!!.title} @${context.eventSourceId}")
}

@io.cratis.chronicle.observation.Reducer
private class PassiveAnnotatedReducer {
    fun hired(event: EmployeeHired) = PassiveEmployee(event.name)
}

@EventSequence("inbox")
@EventSourceType("Employee")
@EventStreamType("Onboarding")
@FilterEventsByTag("hr")
@io.cratis.chronicle.observation.Reducer(isActive = false)
private class FilteredInboxReducer {
    fun hired(event: EmployeeHired) = EmployeeState(event.name, "New")
}

@io.cratis.chronicle.observation.Reducer(isActive = false)
private class ConfidentialReducer {
    fun hired(event: EmployeeHired) = ConfidentialProfile("employee-1", event.name)
}

@io.cratis.chronicle.observation.Reducer
private class ActiveEmployeeReducer {
    fun hired(event: EmployeeHired) = EmployeeState(event.name, "New")
}

private fun appended(
    sequence: Long, source: String, type: String, content: String,
    sourceType: String = "", streamType: String = "", tags: List<String> = emptyList()
): AppendedEvent = AppendedEvent(
    EventContext(
        sequenceNumber = sequence,
        eventSourceId = source,
        eventType = EventTypeDescriptor(EventTypeId(type), EventTypeGeneration.first),
        occurred = Instant.parse("2026-01-01T00:00:00Z"),
        correlationId = UUID.randomUUID(),
        causedBy = Identity.system,
        eventSourceType = sourceType,
        eventStreamType = streamType,
        tags = tags
    ),
    content
)

// Not file-private: ReadModelsService.resolveSubject() invokes the `id` property reflectively via
// KProperty1.call(), which requires the declaring class itself to be JVM-accessible (public).
data class ConfidentialProfile(val id: String, val ssn: String)

// Same visibility requirement as ConfidentialProfile above - its subject property is not `id`.
data class CustomerOrderSummary(val id: String, @Subject val customerId: String, val total: Double)

private fun UUID.toContractGuid(): Bcl.Guid = Bcl.Guid.newBuilder()
    .setLo(java.lang.Long.reverseBytes(mostSignificantBits))
    .setHi(java.lang.Long.reverseBytes(leastSignificantBits))
    .build()

class ReadModelsServiceTests {

    private fun service(
        stub: ReadModelsGrpcKt.ReadModelsCoroutineStub,
        readModelExplorerStub: ReadModelExplorerGrpcKt.ReadModelExplorerCoroutineStub = mockk()
    ): ReadModelsService = ReadModelsService(
        "my-store",
        "default",
        stub,
        mockk<MaterializedReadModelsGrpcKt.MaterializedReadModelsCoroutineStub>(),
        readModelExplorerStub,
        mockk<ComplianceGrpcKt.ComplianceCoroutineStub>()
    )

    @Test
    fun `passive read models register without a sink and active ones retain the configured sink`() = runBlocking {
        val stub = mockk<ReadModelsGrpcKt.ReadModelsCoroutineStub>()
        val registrations = mutableListOf<Readmodels.RegisterManyRequest>()
        coEvery { stub.registerMany(capture(registrations), any()) } returns Empty.getDefaultInstance()

        service(stub).register(PassiveEmployee::class, EmployeeState::class)

        assertEquals(WellKnownSinkTypes.NONE, registrations[0].readModelsList.single().sink.typeId)
        assertEquals(WellKnownSinkTypes.MONGODB, registrations[1].readModelsList.single().sink.typeId)
    }

    @Test
    fun `passive reducer registers without a sink and folds its event types with context`() = runBlocking {
        val stub = mockk<ReadModelsGrpcKt.ReadModelsCoroutineStub>()
        val eventLog = mockk<IEventSequence>()
        val registrations = mutableListOf<Readmodels.RegisterManyRequest>()
        coEvery { stub.registerMany(capture(registrations), any()) } returns Empty.getDefaultInstance()
        coEvery { eventLog.getForEventSourceIdAndEventTypes(any(), any(), any(), any(), any()) } returns listOf(
            appended(2, "employee-1", "EmployeeRenamed", """{"name":"Ada"}"""),
            appended(0, "employee-1", "EmployeeHired", """{"name":"Grace"}"""),
            appended(1, "employee-1", "EmployeePromoted", """{"title":"Senior"}""")
        )
        val service = service(stub).also { it.resolveEventSequence = { eventLog } }
        val job = ReducersService("my-store", "default", ConnectionLifecycle(), mockk(), readModels = service)
            .register(PassiveEmployeeReducer())
        job.cancel()

        assertEquals(EmployeeState("Ada", "Senior @employee-1"), service.getInstanceByKey(EmployeeState::class, "employee-1"))
        coVerify(exactly = 1) {
            eventLog.getForEventSourceIdAndEventTypes(
                "employee-1", listOf(EmployeeHired::class, EmployeePromoted::class, EmployeeRenamed::class),
                null, null, null
            )
        }
        coVerify(exactly = 0) { stub.getInstanceByKey(any(), any()) }
        assertEquals(WellKnownSinkTypes.NONE, registrations.single().readModelsList.single().sink.typeId)
    }

    @Test
    fun `passive reducer returns null for an event source without matching events`() = runBlocking {
        val stub = mockk<ReadModelsGrpcKt.ReadModelsCoroutineStub>()
        val eventLog = mockk<IEventSequence>()
        coEvery { stub.registerMany(any(), any()) } returns Empty.getDefaultInstance()
        coEvery { eventLog.getForEventSourceIdAndEventTypes(any(), any(), any(), any(), any()) } returns emptyList()
        val service = service(stub).also { it.resolveEventSequence = { eventLog } }
        val job = ReducersService("my-store", "default", ConnectionLifecycle(), mockk(), readModels = service)
            .register(PassiveEmployeeReducer())
        job.cancel()

        assertNull(service.getInstanceByKey(EmployeeState::class, "missing"))
        coVerify(exactly = 0) { stub.getInstanceByKey(any(), any()) }
    }

    @Test
    fun `passive reducer folds all event sources up to the event count`() = runBlocking {
        val stub = mockk<ReadModelsGrpcKt.ReadModelsCoroutineStub>()
        val eventLog = mockk<IEventSequence>()
        coEvery { stub.registerMany(any(), any()) } returns Empty.getDefaultInstance()
        coEvery { eventLog.getFromSequenceNumber(any(), any(), any()) } returns listOf(
            appended(2, "employee-1", "EmployeePromoted", """{"title":"Senior"}"""),
            appended(1, "employee-2", "EmployeeHired", """{"name":"Grace"}"""),
            appended(0, "employee-1", "EmployeeHired", """{"name":"Ada"}""")
        )
        val service = service(stub).also { it.resolveEventSequence = { eventLog } }
        val job = ReducersService("my-store", "default", ConnectionLifecycle(), mockk(), readModels = service)
            .register(PassiveEmployeeReducer())
        job.cancel()

        assertEquals(
            listOf(EmployeeState("Ada", "New"), EmployeeState("Grace", "New")),
            service.getInstances(EmployeeState::class, 2)
        )
        assertEquals(
            listOf(EmployeeState("Ada", "Senior"), EmployeeState("Grace", "New")),
            service.getInstances(EmployeeState::class, null)
        )
        assertTrue(service.getInstances(EmployeeState::class, 0).isEmpty())
        coVerify(exactly = 3) {
            eventLog.getFromSequenceNumber(
                EventSequenceNumber.first, null,
                listOf(EmployeeHired::class, EmployeePromoted::class, EmployeeRenamed::class)
            )
        }
        coVerify(exactly = 0) { stub.getAllInstances(any(), any()) }
    }

    @Test
    fun `a passive read model makes its otherwise active reducer passive`() = runBlocking {
        val stub = mockk<ReadModelsGrpcKt.ReadModelsCoroutineStub>()
        val registrations = mutableListOf<Readmodels.RegisterManyRequest>()
        coEvery { stub.registerMany(capture(registrations), any()) } returns Empty.getDefaultInstance()
        val sequence = mockk<IEventSequence>()
        coEvery { sequence.getForEventSourceIdAndEventTypes(any(), any(), any(), any(), any()) } returns listOf(
            appended(0, "employee-1", "EmployeeHired", """{"name":"Ada"}""")
        )
        val service = service(stub).also { it.resolveEventSequence = { sequence } }
        ReducersService("my-store", "default", ConnectionLifecycle(), mockk(), readModels = service)
            .register(PassiveAnnotatedReducer()).cancel()

        assertEquals(WellKnownSinkTypes.NONE, registrations.single().readModelsList.single().sink.typeId)
        assertEquals(PassiveEmployee("Ada"), service.getInstanceByKey(PassiveEmployee::class, "employee-1"))
        coVerify(exactly = 0) { stub.getInstanceByKey(any(), any()) }
    }

    @Test
    fun `passive reducer reads its own sequence and applies source stream and tag filters`() = runBlocking {
        val stub = mockk<ReadModelsGrpcKt.ReadModelsCoroutineStub>()
        coEvery { stub.registerMany(any(), any()) } returns Empty.getDefaultInstance()
        val sequence = mockk<IEventSequence>()
        val requested = mutableListOf<EventSequenceId>()
        coEvery { sequence.getFromSequenceNumber(any(), any(), any()) } returns listOf(
            appended(0, "employee-1", "EmployeeHired", """{"name":"Ignored"}""", "Other", "Onboarding", listOf("hr")),
            appended(1, "employee-1", "EmployeeHired", """{"name":"Ada"}""", "Employee", "Onboarding", listOf("hr")),
            appended(2, "employee-2", "EmployeeHired", """{"name":"Ignored"}""", "Employee", "Other", listOf("hr")),
            appended(3, "employee-3", "EmployeeHired", """{"name":"Ignored"}""", "Employee", "Onboarding")
        )
        coEvery { sequence.getForEventSourceIdAndEventTypes(any(), any(), any(), any(), any()) } returns listOf(
            appended(0, "employee-1", "EmployeeHired", """{"name":"Ignored"}""", "Other", "Onboarding", listOf("hr")),
            appended(1, "employee-1", "EmployeeHired", """{"name":"Ada"}""", "Employee", "Onboarding", listOf("hr"))
        )
        val service = service(stub).also { it.resolveEventSequence = { id -> requested += id; sequence } }
        ReducersService("my-store", "default", ConnectionLifecycle(), mockk(), readModels = service)
            .register(FilteredInboxReducer()).cancel()

        assertEquals(listOf(EmployeeState("Ada", "New")), service.getInstances(EmployeeState::class))
        assertEquals(EmployeeState("Ada", "New"), service.getInstanceByKey(EmployeeState::class, "employee-1"))
        assertEquals(listOf(EventSequenceId("inbox"), EventSequenceId("inbox")), requested)
    }

    @Test
    fun `folded read models release protected values through compliance`() = runBlocking {
        val stub = mockk<ReadModelsGrpcKt.ReadModelsCoroutineStub>()
        val complianceStub = mockk<ComplianceGrpcKt.ComplianceCoroutineStub>()
        coEvery { stub.registerMany(any(), any()) } returns Empty.getDefaultInstance()
        coEvery { complianceStub.release(any(), any()) } answers {
            val request = firstArg<ComplianceOuterClass.ReleaseRequest>()
            ComplianceOuterClass.ReleaseResponse.newBuilder()
                .setPayload(request.payload.replace("111-11-1111", "released")).build()
        }
        val sequence = mockk<IEventSequence>()
        coEvery { sequence.getForEventSourceIdAndEventTypes(any(), any(), any(), any(), any()) } returns listOf(
            appended(0, "employee-1", "EmployeeHired", """{"name":"111-11-1111"}""")
        )
        val service = ReadModelsService("my-store", "default", stub, mockk(), mockk(), complianceStub)
            .also { it.resolveEventSequence = { sequence } }
        ReducersService("my-store", "default", ConnectionLifecycle(), mockk(), readModels = service)
            .register(ConfidentialReducer()).cancel()

        assertEquals(ConfidentialProfile("employee-1", "released"), service.getInstanceByKey(ConfidentialProfile::class, "employee-1"))
        coVerify(exactly = 1) { complianceStub.release(any(), any()) }
    }

    @Test
    fun `active reducer retains the configured sink and uses the kernel for reads`() = runBlocking {
        val stub = mockk<ReadModelsGrpcKt.ReadModelsCoroutineStub>()
        val registrations = mutableListOf<Readmodels.RegisterManyRequest>()
        coEvery { stub.registerMany(capture(registrations), any()) } returns Empty.getDefaultInstance()
        coEvery { stub.getInstanceByKey(any(), any()) } returns Readmodels.GetInstanceByKeyResponse.newBuilder()
            .setReadModel("""{"name":"Ada","title":"New"}""")
            .build()
        val service = service(stub)
        val job = ReducersService("my-store", "default", ConnectionLifecycle(), mockk(), readModels = service)
            .register(ActiveEmployeeReducer())
        job.cancel()

        assertEquals(EmployeeState("Ada", "New"), service.getInstanceByKey(EmployeeState::class, "employee-1"))
        coVerify(exactly = 1) { stub.getInstanceByKey(any(), any()) }
        assertEquals(WellKnownSinkTypes.MONGODB, registrations.single().readModelsList.single().sink.typeId)
    }

    @Test
    fun `getSnapshotsById deserializes the read model json into the caller's type, not the raw proto`() = runBlocking {
        val stub = mockk<ReadModelsGrpcKt.ReadModelsCoroutineStub>()
        val readModelExplorerStub = mockk<ReadModelExplorerGrpcKt.ReadModelExplorerCoroutineStub>()
        val correlationId = UUID.randomUUID()
        val occurred = Instant.parse("2026-01-01T00:00:00Z")

        val snapshot = Readmodelexplorer.ReadModelSnapshotResponse.newBuilder()
            .setInstance("""{"name":"Ada","title":"Engineer"}""")
            .setOccurred(Readmodelexplorer.SerializableDateTimeOffset.newBuilder().setValue(occurred.toString()))
            .setCorrelationId(correlationId.toContractGuid())
            .build()
        coEvery { readModelExplorerStub.allSnapshotsForReadModel(any(), any()) } returns
            Readmodelexplorer.QueryResult_IEnumerable_ReadModelSnapshotResponse.newBuilder()
                .addData(snapshot)
                .setIsAuthorized(true)
                .build()

        val result = service(stub, readModelExplorerStub).getSnapshotsById(EmployeeState::class, "employee-1")

        assertEquals(1, result.size)
        val single = result.single()
        assertEquals(EmployeeState("Ada", "Engineer"), single.instance)
        assertEquals(correlationId, single.correlationId)
        assertEquals(occurred, single.occurred)
    }

    @Test
    fun `watch deserializes each emission's read model json into the caller's type`() = runBlocking {
        val stub = mockk<ReadModelsGrpcKt.ReadModelsCoroutineStub>()
        val changeset = Readmodels.ReadModelChangeset.newBuilder()
            .setNamespace("default")
            .setModelKey("employee-1")
            .setReadModel("""{"name":"Ada","title":"Principal Engineer"}""")
            .setRemoved(false)
            .setChangeType(Readmodels.ReadModelChangeType.Modified)
            .setEventSequenceNumber(5)
            .build()
        every { stub.watch(any(), any()) } returns flowOf(changeset)

        val result = service(stub).watch(EmployeeState::class).toList()

        assertEquals(1, result.size)
        val single = result.single()
        assertEquals(EmployeeState("Ada", "Principal Engineer"), single.readModel)
        assertEquals("employee-1", single.modelKey)
        assertEquals(ReadModelChangeType.Modified, single.changeType)
        assertEquals(5L, single.eventSequenceNumber)
        assertTrue(!single.removed)
    }

    @Test
    fun `watch surfaces a null read model when the change removed the instance`() = runBlocking {
        val stub = mockk<ReadModelsGrpcKt.ReadModelsCoroutineStub>()
        val changeset = Readmodels.ReadModelChangeset.newBuilder()
            .setNamespace("default")
            .setModelKey("employee-1")
            .setRemoved(true)
            .setChangeType(Readmodels.ReadModelChangeType.Removed)
            .build()
        every { stub.watch(any(), any()) } returns flowOf(changeset)

        val result = service(stub).watch(EmployeeState::class).toList()

        assertNull(result.single().readModel)
        assertEquals(ReadModelChangeType.Removed, result.single().changeType)
    }

    @Test
    fun `releaseMany releases every instance individually, preserving order`() = runBlocking {
        val stub = mockk<ReadModelsGrpcKt.ReadModelsCoroutineStub>()
        val complianceStub = mockk<ComplianceGrpcKt.ComplianceCoroutineStub>()
        coEvery { complianceStub.release(any(), any()) } answers {
            val request = firstArg<ComplianceOuterClass.ReleaseRequest>()
            ComplianceOuterClass.ReleaseResponse.newBuilder().setPayload(request.payload).build()
        }

        val service = ReadModelsService(
            "my-store",
            "default",
            stub,
            mockk<MaterializedReadModelsGrpcKt.MaterializedReadModelsCoroutineStub>(),
            mockk<ReadModelExplorerGrpcKt.ReadModelExplorerCoroutineStub>(),
            complianceStub
        )

        val instances = listOf(
            ConfidentialProfile("employee-1", "111-11-1111"),
            ConfidentialProfile("employee-2", "222-22-2222")
        )

        val released = service.releaseMany(instances)

        assertEquals(instances, released)
        coVerify(exactly = 2) { complianceStub.release(any(), any()) }
    }

    @Test
    fun `release resolves the subject from a property annotated Subject rather than id`() = runBlocking {
        val stub = mockk<ReadModelsGrpcKt.ReadModelsCoroutineStub>()
        val complianceStub = mockk<ComplianceGrpcKt.ComplianceCoroutineStub>()
        var capturedSubject: String? = null
        coEvery { complianceStub.release(any(), any()) } answers {
            val request = firstArg<ComplianceOuterClass.ReleaseRequest>()
            capturedSubject = request.subject
            ComplianceOuterClass.ReleaseResponse.newBuilder().setPayload(request.payload).build()
        }

        val service = ReadModelsService(
            "my-store",
            "default",
            stub,
            mockk<MaterializedReadModelsGrpcKt.MaterializedReadModelsCoroutineStub>(),
            mockk<ReadModelExplorerGrpcKt.ReadModelExplorerCoroutineStub>(),
            complianceStub
        )

        service.release(CustomerOrderSummary(id = "order-1", customerId = "customer-42", total = 99.5))

        assertEquals("customer-42", capturedSubject)
    }

    @Test
    fun `release falls back to the id property when nothing is annotated Subject`() = runBlocking {
        val stub = mockk<ReadModelsGrpcKt.ReadModelsCoroutineStub>()
        val complianceStub = mockk<ComplianceGrpcKt.ComplianceCoroutineStub>()
        var capturedSubject: String? = null
        coEvery { complianceStub.release(any(), any()) } answers {
            val request = firstArg<ComplianceOuterClass.ReleaseRequest>()
            capturedSubject = request.subject
            ComplianceOuterClass.ReleaseResponse.newBuilder().setPayload(request.payload).build()
        }

        val service = ReadModelsService(
            "my-store",
            "default",
            stub,
            mockk<MaterializedReadModelsGrpcKt.MaterializedReadModelsCoroutineStub>(),
            mockk<ReadModelExplorerGrpcKt.ReadModelExplorerCoroutineStub>(),
            complianceStub
        )

        service.release(ConfidentialProfile(id = "employee-1", ssn = "111-11-1111"))

        assertEquals("employee-1", capturedSubject)
    }

    @Test
    fun `release resolves Subject from a Java record's component, not just a Kotlin property`() = runBlocking {
        val stub = mockk<ReadModelsGrpcKt.ReadModelsCoroutineStub>()
        val complianceStub = mockk<ComplianceGrpcKt.ComplianceCoroutineStub>()
        var capturedSubject: String? = null
        coEvery { complianceStub.release(any(), any()) } answers {
            val request = firstArg<ComplianceOuterClass.ReleaseRequest>()
            capturedSubject = request.subject
            ComplianceOuterClass.ReleaseResponse.newBuilder().setPayload(request.payload).build()
        }

        val service = ReadModelsService(
            "my-store",
            "default",
            stub,
            mockk<MaterializedReadModelsGrpcKt.MaterializedReadModelsCoroutineStub>(),
            mockk<ReadModelExplorerGrpcKt.ReadModelExplorerCoroutineStub>(),
            complianceStub
        )

        service.release(JavaCustomerOrderSummary("order-1", "customer-42", "pending"))

        assertEquals("customer-42", capturedSubject)
    }
}
