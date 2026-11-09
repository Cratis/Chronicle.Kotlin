// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import com.google.gson.JsonObject
import io.cratis.chronicle.sinks.WellKnownSinkTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GraphDefinitionBuilderTests {
    private val registration = OrganizationGraphs.modelBound()
    private val snapshot = GraphSnapshot.of(registration)
    private val schema = snapshot.getAsJsonObject("readModel").getAsJsonObject("schema")
    private val nodes = snapshot.getAsJsonObject("projection").getAsJsonObject("graph").getAsJsonObject("nodes")
    private val edges = snapshot.getAsJsonObject("projection").getAsJsonObject("graph").getAsJsonObject("edges")

    private fun JsonObject.obj(name: String): JsonObject = getAsJsonObject(name)

    private fun JsonObject.text(name: String): String = get(name).asString

    private fun JsonObject.property(element: String, name: String): JsonObject =
        obj("properties").obj(element).obj("properties").obj(name)

    @Test
    fun `builds the definition the dotnet client builds from annotations`() {
        assertEquals(OrganizationGraphs.expected, snapshot)
    }

    @Test
    fun `builds the definition the dotnet client builds from a fluent builder`() {
        assertEquals(OrganizationGraphs.expected, GraphSnapshot.of(OrganizationGraphs.fluent()))
    }

    @Test
    fun `the same graph declared both ways is identical`() {
        assertEquals(snapshot, GraphSnapshot.of(OrganizationGraphs.fluent()))
        assertEquals("Organization", OrganizationGraphs.fluent().graphName)
    }

    @Test
    fun `names the read model after the graph`() {
        assertEquals("Organization", registration.readModel.type.identifier)
        assertEquals("Organization", registration.readModel.containerName)
        assertEquals("Organization", registration.projection.readModel)
    }

    @Test
    fun `stores the graph in the sink it is given`() {
        assertEquals("Neo4j", registration.readModel.sink.typeId)
        assertEquals(
            WellKnownSinkTypes.IN_MEMORY_GRAPH,
            OrganizationGraphs.modelBound(WellKnownSinkTypes.IN_MEMORY_GRAPH).readModel.sink.typeId
        )
    }

    @Test
    fun `makes the read model observed by the projection`() {
        assertEquals(2, registration.readModel.observerTypeValue)
        assertEquals(registration.projection.identifier, registration.readModel.observerIdentifier)
    }

    @Test
    fun `makes the projection a graph projection over the event log`() {
        assertTrue(registration.projection.hasGraph())
        assertTrue(registration.projection.hasAll())
        assertEquals("event-log", registration.projection.eventSequenceId)
        assertTrue(registration.projection.isActive)
        assertTrue(registration.projection.isRewindable)
        assertEquals("{}", registration.projection.initialModelState)
    }

    @Test
    fun `marks the schema as a graph`() {
        assertTrue(schema.get("x-graph").asBoolean)
        assertEquals(setOf("Group", "MEMBER_OF", "Person", "Place", "VISITED"), schema.obj("properties").keySet())
    }

    @Test
    fun `describes the key of a node`() {
        assertEquals("personId", schema.obj("properties").obj("Person").text("x-graph-key"))
        assertEquals("Node", schema.obj("properties").obj("Person").text("x-graph-kind"))
    }

    @Test
    fun `describes the end nodes of an edge`() {
        val edge = schema.obj("properties").obj("MEMBER_OF")
        assertEquals("Edge", edge.text("x-graph-kind"))
        assertEquals("Person", edge.text("x-graph-source"))
        assertEquals("Group", edge.text("x-graph-target"))
    }

    @Test
    fun `describes the identity of an edge`() {
        assertEquals("visitId", schema.obj("properties").obj("VISITED").text("x-graph-identity"))
        assertFalse(schema.obj("properties").obj("MEMBER_OF").has("x-graph-identity"))
    }

    @Test
    fun `leaves the keys of the end nodes out of the properties of an edge`() {
        assertEquals(setOf("since"), schema.obj("properties").obj("MEMBER_OF").obj("properties").keySet())
    }

    @Test
    fun `describes properties with the formats the dotnet client sends`() {
        assertEquals("guid", schema.property("Person", "personId").text("format"))
        assertEquals("int32", schema.property("Group", "memberCount").text("format"))
        assertEquals("date-time-offset", schema.property("MEMBER_OF", "since").text("format"))
    }

    @Test
    fun `keys a node by the event source id by default`() {
        assertEquals("\$eventSourceId", nodes.obj("Person").obj("from").obj("PersonRegistered+1").text("key"))
    }

    @Test
    fun `keys a node by an event property`() {
        assertEquals("groupId", nodes.obj("Group").obj("from").obj("MemberAdded+1").text("key"))
    }

    @Test
    fun `maps properties by name and never the key`() {
        val properties = nodes.obj("Person").obj("from").obj("PersonRegistered+1").obj("properties")
        assertEquals("name", properties.text("name"))
        assertFalse(properties.has("personId"))
    }

    @Test
    fun `detaches when asked to`() {
        assertTrue(nodes.obj("Person").obj("removedWith").obj("PersonRemoved+1").get("detach").asBoolean)
    }

    @Test
    fun `counts up and down on a node`() {
        assertEquals("\$increment", nodes.obj("Group").obj("from").obj("MemberAdded+1").obj("properties").text("memberCount"))
        assertEquals("\$decrement", nodes.obj("Group").obj("from").obj("MemberRemoved+1").obj("properties").text("memberCount"))
    }

    @Test
    fun `resolves the keys of an edge`() {
        val from = edges.obj("MEMBER_OF").obj("from").obj("MemberAdded+1")
        assertEquals("personId", from.text("sourceKey"))
        assertEquals("groupId", from.text("targetKey"))
    }

    @Test
    fun `maps an edge property from the event context`() {
        assertEquals(
            "\$eventContext(occurred)",
            edges.obj("MEMBER_OF").obj("from").obj("MemberAdded+1").obj("properties").text("since")
        )
    }

    @Test
    fun `removes an edge by the keys of its end nodes`() {
        assertEquals("groupId", edges.obj("MEMBER_OF").obj("removedWith").obj("MemberRemoved+1").text("targetKey"))
    }

    @Test
    fun `takes the identity of an edge from the event`() {
        val from = edges.obj("VISITED").obj("from").obj("PlaceVisited+1")
        assertEquals("visitId", from.text("identity"))
        assertEquals("\$count", from.obj("properties").text("times"))
        assertFalse(from.obj("properties").has("visitId"))
    }

    @Test
    fun `survives a round trip over the wire`() {
        val parsed = Cratis.Chronicle.Contracts.Projections.ProjectionsOuterClass.ProjectionDefinition.parseFrom(
            registration.projection.toByteArray()
        )
        assertEquals(registration.projection, parsed)
        assertEquals(setOf("Group", "Person", "Place"), parsed.graph.nodesMap.keys)
    }
}
