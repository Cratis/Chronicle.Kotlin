// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

@file:Suppress("DEPRECATION")

package io.cratis.chronicle.concepts

import io.cratis.chronicle.json.chronicleGson
import io.cratis.fundamentals.execution.CorrelationId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

private data class LegacyBookId(private val id: String) : io.cratis.chronicle.concepts.ConceptAs<String> {
    override fun value(): String = id
}

class DeprecatedAliasesTests {
    @Test
    fun `the deprecated concept alias still serializes as its value`() {
        assertEquals("\"dune\"", chronicleGson.toJson(LegacyBookId("dune")))
    }

    @Test
    fun `the deprecated correlation id alias creates a Fundamentals correlation id`() {
        assertInstanceOf(CorrelationId::class.java, io.cratis.chronicle.correlation.CorrelationId.create())
    }
}
