// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import Cratis.Chronicle.Contracts.Projections.ProjectionsGrpcKt
import Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass
import Cratis.Chronicle.Contracts.ReadModels.Readmodels
import com.google.protobuf.Empty
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.ProjectionsService
import io.cratis.chronicle.projections.graphs.fluent.OrganizationGraph
import io.cratis.chronicle.projections.graphs.modelbound.Group
import io.cratis.chronicle.projections.graphs.modelbound.MemberOf
import io.cratis.chronicle.projections.graphs.modelbound.Person
import io.cratis.chronicle.projections.graphs.modelbound.Place
import io.cratis.chronicle.projections.graphs.modelbound.Visited
import io.cratis.chronicle.readModels.ReadModel
import io.cratis.chronicle.readModels.ReadModelsService
import io.cratis.chronicle.sinks.WellKnownSinkTypes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
private data class NoteWritten(val text: String)

@ReadModel
@FromEvent(NoteWritten::class)
private data class Note(val text: String = "")

class GraphRegistrationTests {
    private val stub = mockk<ProjectionsGrpcKt.ProjectionsCoroutineStub>()
    private val readModels = mockk<ReadModelsService>(relaxed = true)
    private val request = slot<ProjectionsOuterClass.RegisterRequest>()
    private val definitions = slot<List<Readmodels.ReadModelDefinition>>()
    private val service = ProjectionsService("my-store", stub, readModels, "default")

    init {
        coEvery { stub.register(capture(request), any()) } returns Empty.getDefaultInstance()
        coEvery { readModels.registerDefinitions(capture(definitions)) } returns Unit
    }

    private fun register(vararg declarations: Any) = runBlocking { service.register(*declarations) }

    private val annotated = arrayOf(Person::class, Group::class, Place::class, MemberOf::class, Visited::class)

    @Test
    fun `registers an annotated graph as one projection and one read model`() {
        register(*annotated)

        val projection = request.captured.projectionsList.single()
        assertTrue(projection.hasGraph())
        assertEquals("Organization", projection.identifier)
        assertEquals(setOf("Group", "Person", "Place"), projection.graph.nodesMap.keys)
        assertEquals(setOf("MEMBER_OF", "VISITED"), projection.graph.edgesMap.keys)

        val readModel = definitions.captured.single()
        assertEquals("Organization", readModel.type.identifier)
        assertEquals(WellKnownSinkTypes.NEO4J, readModel.sink.typeId)
        assertTrue(readModel.schema.contains("\"x-graph\":true"))
    }

    @Test
    fun `registers a fluent graph the same way`() {
        register(OrganizationGraph())

        val projection = request.captured.projectionsList.single()
        assertTrue(projection.hasGraph())
        assertEquals(OrganizationGraph::class.qualifiedName, projection.identifier)
        assertEquals("Organization", definitions.captured.single().type.identifier)
    }

    @Test
    fun `registers the read model of a graph before the projection that fills it`() {
        register(*annotated)

        coVerifyOrder {
            readModels.registerDefinitions(any())
            stub.register(any(), any())
        }
    }

    @Test
    fun `registers the graph with the sink it is configured to use`() {
        service.graphSinkTypeId = WellKnownSinkTypes.IN_MEMORY_GRAPH

        register(*annotated)

        assertEquals(WellKnownSinkTypes.IN_MEMORY_GRAPH, definitions.captured.single().sink.typeId)
    }

    @Test
    fun `registers a graph next to the projections of ordinary read models`() {
        register(Note::class, *annotated)

        val projections = request.captured.projectionsList
        assertEquals(setOf("Note", "Organization"), projections.map { it.identifier }.toSet())
        assertFalse(projections.single { it.identifier == "Note" }.hasGraph())
        assertTrue(projections.single { it.identifier == "Organization" }.hasGraph())
    }

    @Test
    fun `never registers a node as a model-bound read model`() {
        register(Person::class)

        val projection = request.captured.projectionsList.single()
        assertTrue(projection.hasGraph())
        assertEquals(0, projection.fromCount)
        coVerify(exactly = 0) { readModels.registerWithObserver(any(), any(), any(), any()) }
    }

    @Test
    fun `registers nothing when there is nothing to register`() {
        register(NoteWritten::class)

        coVerify(exactly = 0) { stub.register(any(), any()) }
        coVerify(exactly = 0) { readModels.registerDefinitions(any()) }
    }
}
