// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import Cratis.Chronicle.Contracts.Compliance.ComplianceGrpcKt
import Cratis.Chronicle.Contracts.ReadModels.MaterializedReadModelsGrpcKt
import Cratis.Chronicle.Contracts.ReadModels.ReadModelsGrpcKt
import Cratis.Chronicle.Contracts.ReadModels.Readmodels
import bcl.Bcl
import io.mockk.coEvery
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

private fun UUID.toContractGuid(): Bcl.Guid = Bcl.Guid.newBuilder()
    .setLo(java.lang.Long.reverseBytes(mostSignificantBits))
    .setHi(java.lang.Long.reverseBytes(leastSignificantBits))
    .build()

class ReadModelsServiceTests {

    private fun service(stub: ReadModelsGrpcKt.ReadModelsCoroutineStub): ReadModelsService = ReadModelsService(
        "my-store",
        "default",
        stub,
        mockk<MaterializedReadModelsGrpcKt.MaterializedReadModelsCoroutineStub>(),
        mockk<ComplianceGrpcKt.ComplianceCoroutineStub>()
    )

    @Test
    fun `getSnapshotsById deserializes the read model json into the caller's type, not the raw proto`() = runBlocking {
        val stub = mockk<ReadModelsGrpcKt.ReadModelsCoroutineStub>()
        val correlationId = UUID.randomUUID()
        val occurred = Instant.parse("2026-01-01T00:00:00Z")

        val snapshot = Readmodels.ReadModelSnapshot.newBuilder()
            .setReadModel("""{"name":"Ada","title":"Engineer"}""")
            .setOccurred(Readmodels.SerializableDateTimeOffset.newBuilder().setValue(occurred.toString()))
            .setCorrelationId(correlationId.toContractGuid())
            .build()
        coEvery { stub.getSnapshotsByKey(any(), any()) } returns Readmodels.GetSnapshotsByKeyResponse.newBuilder()
            .addSnapshots(snapshot)
            .build()

        val result = service(stub).getSnapshotsById(EmployeeState::class, "employee-1")

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
}
