// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs.views

import io.cratis.chronicle.projections.graphs.GraphViews
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GraphViewConformanceTests {
    private val fixture: String = GraphViewConformanceTests::class.java.getResourceAsStream("/graphs/graph-views-organization.json")
        ?.use { it.readBytes().toString(Charsets.UTF_8) }
        ?: error("The shared graph views fixture is missing from the test resources.")

    private val definition = GraphViews.definitionsFor(*organizationViewDeclarations.toTypedArray()).single()

    @Test
    fun `the schema-included form is the fixture byte for byte`() {
        assertEquals(fixture, definition.toJson(includeSchema = true))
    }

    @Test
    fun `the default form is the fixture without its schema`() {
        val withoutSchema = fixture.replace(Regex("""  "schema": \{.*?\n  \},\n""", RegexOption.DOT_MATCHES_ALL), "")

        assertTrue(withoutSchema.length < fixture.length)
        assertEquals(withoutSchema, definition.toJson())
    }

    @Test
    fun `the definition is for the graph the views read`() {
        assertEquals("Organization", definition.graph)
        assertEquals(8, definition.views.size)
    }

    @Test
    fun `takes Java classes as well as Kotlin classes`() {
        val declarations = organizationViewDeclarations.map { (it as kotlin.reflect.KClass<*>).java }

        assertEquals(definition, GraphViews.definitionsFor(*declarations.toTypedArray()).single())
    }

    @Test
    fun `ignores everything that is not part of a graph`() {
        assertTrue(GraphViews.definitionsFor(String::class, Int::class.java).isEmpty())
    }
}
