// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import Cratis.Chronicle.Contracts.Projections.ProjectionsGrpcKt
import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import io.cratis.chronicle.eventSequences.EventSequenceId
import io.cratis.chronicle.readModels.ReadModelsService
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private data class EmployeeName(val name: String = "")

/**
 * A registered projection is the answer when a read model is part of the system. This is the other
 * case: a question of the event log that nobody wants to deploy a projection for.
 */
class ProjectionQueryTests {

    private val declaration = "from EmployeeHired\n    set name to firstName"

    private fun serviceReturning(
        response: ProjectionsOuterClass.OneOf_ProjectionPreview_ProjectionDeclarationParsingErrors,
        request: io.mockk.CapturingSlot<ProjectionsOuterClass.PreviewProjectionRequest>? = null
    ): ProjectionsService {
        val stub = mockk<ProjectionsGrpcKt.ProjectionsCoroutineStub>()
        if (request != null) {
            coEvery { stub.preview(capture(request), any()) } returns response
        } else {
            coEvery { stub.preview(any(), any()) } returns response
        }
        return ProjectionsService("my-store", stub, mockk<ReadModelsService>(), "default")
    }

    private fun projected(vararg entries: String) =
        ProjectionsOuterClass.OneOf_ProjectionPreview_ProjectionDeclarationParsingErrors.newBuilder()
            .setValue0(
                ProjectionsOuterClass.ProjectionPreview.newBuilder()
                    .addAllReadModelEntries(entries.toList())
            )
            .build()

    private fun parsingErrors() =
        ProjectionsOuterClass.OneOf_ProjectionPreview_ProjectionDeclarationParsingErrors.newBuilder()
            .setValue1(
                ProjectionsOuterClass.ProjectionDeclarationParsingErrors.newBuilder()
                    .addErrors(
                        ProjectionsOuterClass.ProjectionDeclarationSyntaxError.newBuilder()
                            .setMessage("unknown event type 'EmployeeHried'")
                            .setLine(1)
                            .setColumn(6)
                    )
            )
            .build()

    @Test
    fun `a declaration that parses comes back with what it projected`() = runBlocking {
        val result = serviceReturning(projected("""{"name":"Ada"}""", """{"name":"Grace"}""")).query(declaration)

        val projection = assertInstanceOf(ProjectionQueryResult.Projected::class.java, result)
        assertTrue(projection.isSuccess)
        assertEquals(listOf("""{"name":"Ada"}""", """{"name":"Grace"}"""), projection.entries)
    }

    @Test
    fun `the entries deserialize into whatever shape the declaration produced`() = runBlocking {
        val result = serviceReturning(projected("""{"name":"Ada"}""", """{"name":"Grace"}""")).query(declaration)

        val instances = (result as ProjectionQueryResult.Projected).instancesOf(EmployeeName::class)
        assertEquals(listOf(EmployeeName("Ada"), EmployeeName("Grace")), instances)
    }

    @Test
    fun `a declaration that projects nothing is still a success`() = runBlocking {
        val result = serviceReturning(projected()).query(declaration)

        assertTrue(result.isSuccess)
        assertTrue((result as ProjectionQueryResult.Projected).entries.isEmpty())
    }

    @Test
    fun `a declaration the kernel cannot parse comes back as errors rather than throwing`() = runBlocking {
        val result = serviceReturning(parsingErrors()).query(declaration)

        val invalid = assertInstanceOf(ProjectionQueryResult.Invalid::class.java, result)
        assertFalse(invalid.isSuccess)
        assertEquals("unknown event type 'EmployeeHried'", invalid.errors.single().message)
        assertEquals(1, invalid.errors.single().line)
        assertEquals(6, invalid.errors.single().column)
    }

    @Test
    fun `an error reads as line, column and message`() = runBlocking {
        val result = serviceReturning(parsingErrors()).query(declaration) as ProjectionQueryResult.Invalid

        assertEquals("1:6: unknown event type 'EmployeeHried'", result.errors.single().toString())
    }

    @Test
    fun `the declaration and the sequence go to the kernel as given`() = runBlocking {
        val request = slot<ProjectionsOuterClass.PreviewProjectionRequest>()
        serviceReturning(projected(), request).query(declaration, EventSequenceId("audit"))

        assertEquals(declaration, request.captured.declaration)
        assertEquals("audit", request.captured.eventSequenceId)
        assertEquals("my-store", request.captured.eventStore)
        assertEquals("default", request.captured.namespace)
    }

    @Test
    fun `the event log is what a query runs over unless told otherwise`() = runBlocking {
        val request = slot<ProjectionsOuterClass.PreviewProjectionRequest>()
        serviceReturning(projected(), request).query(declaration)

        assertEquals(EventSequenceId.eventLog.value, request.captured.eventSequenceId)
    }
}
