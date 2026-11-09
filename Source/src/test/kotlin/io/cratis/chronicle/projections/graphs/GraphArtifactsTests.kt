// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import io.cratis.chronicle.artifacts.ClientArtifacts
import io.cratis.chronicle.artifacts.KnownClientArtifacts
import io.cratis.chronicle.projections.graphs.fluent.OrganizationGraph
import io.cratis.chronicle.projections.graphs.modelbound.Group
import io.cratis.chronicle.projections.graphs.modelbound.MemberOf
import io.cratis.chronicle.projections.graphs.modelbound.Person
import io.cratis.chronicle.projections.graphs.modelbound.Place
import io.cratis.chronicle.projections.graphs.modelbound.Visited
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GraphArtifactsTests {
    private val elements = setOf(Person::class, Group::class, Place::class, MemberOf::class, Visited::class)

    @Test
    fun `sorts annotated nodes and edges into graph elements`() {
        val artifacts = KnownClientArtifacts(Person::class, Group::class, MemberOf::class, Visited::class, Place::class)

        assertEquals(elements, artifacts.graphElements.toSet())
    }

    @Test
    fun `does not take a node for a model-bound read model`() {
        val artifacts = KnownClientArtifacts(Person::class, Group::class, Place::class)

        assertTrue(artifacts.modelBoundProjections.isEmpty())
        assertTrue(artifacts.readModels.isEmpty())
    }

    @Test
    fun `sorts a fluent graph into graph projections`() {
        val artifacts = KnownClientArtifacts(OrganizationGraph::class)

        assertEquals(listOf(OrganizationGraph::class), artifacts.graphProjections)
        assertTrue(artifacts.graphElements.isEmpty())
    }

    @Test
    fun `discovers graph elements and fluent graphs by scanning`() {
        val artifacts = ClientArtifacts(
            "io.cratis.chronicle.projections.graphs.modelbound",
            "io.cratis.chronicle.projections.graphs.fluent"
        )

        assertTrue(artifacts.graphElements.containsAll(elements))
        assertTrue(artifacts.graphElements.contains(io.cratis.chronicle.projections.graphs.fluent.MemberOf::class))
        assertEquals(listOf(OrganizationGraph::class), artifacts.graphProjections)
        assertTrue(artifacts.modelBoundProjections.none { it in elements })
    }

    @Test
    fun `an artifact source that knows nothing about graphs lists none`() {
        val artifacts = object : io.cratis.chronicle.artifacts.IClientArtifacts by KnownClientArtifacts.empty {}

        assertTrue(artifacts.graphElements.isEmpty())
        assertTrue(artifacts.graphProjections.isEmpty())
    }
}
