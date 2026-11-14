// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs.views

import Cratis.Chronicle.Contracts.Projections.ProjectionsGrpcKt
import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import Cratis.Chronicle.Contracts.ReadModels.Readmodels
import com.google.protobuf.Empty
import io.cratis.chronicle.artifacts.ClientArtifacts
import io.cratis.chronicle.artifacts.KnownClientArtifacts
import io.cratis.chronicle.projections.ProjectionsService
import io.cratis.chronicle.projections.graphs.GraphProjections
import io.cratis.chronicle.projections.graphs.GraphSnapshot
import io.cratis.chronicle.projections.graphs.ViewOf
import io.cratis.chronicle.projections.graphs.ViewRootNotANode
import io.cratis.chronicle.readModels.ReadModelsService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private class NotAGraphElement

@ViewOf(NotAGraphElement::class)
private data class BrokenView(val name: String)

class GraphViewRegistrationTests {
    private val stub = mockk<ProjectionsGrpcKt.ProjectionsCoroutineStub>()
    private val readModels = mockk<ReadModelsService>(relaxed = true)
    private val request = slot<ProjectionsOuterClass.RegisterRequest>()
    private val definitions = slot<List<Readmodels.ReadModelDefinition>>()
    private val service = ProjectionsService("my-store", stub, readModels, "default")

    init {
        coEvery { stub.register(capture(request), any()) } returns Empty.getDefaultInstance()
        coEvery { readModels.registerDefinitions(capture(definitions)) } returns Unit
    }

    private val graph = listOf(Person::class, Group::class, MemberOf::class, PartOf::class, HasRole::class)

    private fun register(vararg declarations: Any) = runBlocking { service.register(*declarations) }

    @Test
    fun `a graph without views has no views and registers as it always did`() {
        val declarations = GraphProjections("Neo4j").declarationsFor(graph)

        assertTrue(declarations.viewFailures.isEmpty())
        assertTrue(declarations.registrations.single().views.views.isEmpty())
    }

    @Test
    fun `views are not part of what the kernel is told about the graph`() {
        val without = GraphProjections("Neo4j").registrationsFor(graph).single()
        val with = GraphProjections("Neo4j").registrationsFor(organizationViewDeclarations).single()

        assertEquals(GraphSnapshot.of(without), GraphSnapshot.of(with))
        assertEquals(8, with.views.views.size)
    }

    @Test
    fun `registering views alongside a graph registers the graph as before`() {
        register(*organizationViewDeclarations.toTypedArray())

        assertEquals(listOf("Organization"), request.captured.projectionsList.map { it.identifier })
        assertTrue(request.captured.projectionsList.single().hasGraph())
    }

    @Test
    fun `an invalid view does not stop a graph from registering, and is reported afterwards`() {
        val failure = assertThrows(ViewRootNotANode::class.java) { register(*graph.toTypedArray(), BrokenView::class) }

        assertEquals("ViewRootNotANode", failure.reason)
        coVerify(exactly = 1) { stub.register(any(), any()) }
        assertEquals(listOf("Organization"), request.captured.projectionsList.map { it.identifier })
    }

    @Test
    fun `an invalid view leaves the valid views of the graph in place`() {
        val declarations = GraphProjections("Neo4j").declarationsFor(organizationViewDeclarations + BrokenView::class)

        assertEquals(1, declarations.viewFailures.size)
        assertEquals(8, declarations.registrations.single().views.views.size)
    }

    @Test
    fun `views are discovered as artifacts`() {
        val known = KnownClientArtifacts(Person::class, PersonWithGroups::class, GroupSummary::class)
        val scanned = ClientArtifacts("io.cratis.chronicle.projections.graphs.views")

        assertEquals(setOf(PersonWithGroups::class, GroupSummary::class), known.graphViews.toSet())
        assertTrue(scanned.graphViews.containsAll(listOf(PersonWithGroups::class, GroupSummary::class, RoleAssignment::class)))
        assertTrue(known.graphElements.none { it in known.graphViews })
    }
}
