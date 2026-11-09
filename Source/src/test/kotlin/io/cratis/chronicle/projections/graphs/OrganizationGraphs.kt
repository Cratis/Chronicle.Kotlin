// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import io.cratis.chronicle.projections.graphs.fluent.OrganizationGraph
import io.cratis.chronicle.projections.graphs.modelbound.Group
import io.cratis.chronicle.projections.graphs.modelbound.MemberOf
import io.cratis.chronicle.projections.graphs.modelbound.Person
import io.cratis.chronicle.projections.graphs.modelbound.Place
import io.cratis.chronicle.projections.graphs.modelbound.Visited

/** The Organization graph as every test sees it: built from annotations, built from a builder, and the expected definition. */
internal object OrganizationGraphs {
    private val modelBoundTypes = listOf(
        Person::class,
        Group::class,
        Place::class,
        MemberOf::class,
        Visited::class
    )

    /** What the annotated types register, stored in a sink of type [sinkTypeId]. */
    fun modelBound(sinkTypeId: String = "Neo4j"): GraphRegistration =
        GraphProjections(sinkTypeId).registrationsFor(modelBoundTypes).single()

    /**
     * What the fluent [OrganizationGraph] registers, stored in a sink of type [sinkTypeId]. It is identified like
     * the annotated graph, so the two compare equal - a fluent graph is otherwise identified by its class.
     */
    fun fluent(sinkTypeId: String = "Neo4j"): GraphRegistration =
        GraphDefinitionBuilder.build(GraphProjectionBuilder().build(OrganizationGraph()), "Organization", sinkTypeId)

    /**
     * The definition the .NET client builds for the same graph, written out with the wire names of the
     * contract (see `Source/Clients/DotNET.Specs/Projections/Graphs/for_GraphDefinitionBuilder`). It is the
     * definition the Elixir client's spec holds its own implementation to, with the event type identifiers
     * and property formats the Kotlin types produce.
     */
    val expected: JsonObject by lazy {
        val stream = OrganizationGraphs::class.java.getResourceAsStream("/graphs/organization.json")
            ?: error("The expected Organization graph definition is missing from the test resources.")
        stream.use { JsonParser.parseReader(it.reader()).asJsonObject }
    }
}
