// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.keys

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private data class OrderLineAdded(val orderId: String, val product: String, val quantity: Int)

class KeyBuilderTests {

    @Test
    fun `a key builder with nothing called resolves to the event source id`() {
        val builder = KeyBuilder<OrderLineAdded>()
        assertEquals(ResolvedKey.EventSourceId, builder.build())
    }

    @Test
    fun `usingKey resolves to the name of the given property`() {
        val builder = KeyBuilder<OrderLineAdded>()
        builder.usingKey(OrderLineAdded::orderId)
        assertEquals(ResolvedKey.Property("orderId"), builder.build())
    }

    @Test
    fun `usingKeyWithPropertyName resolves to the given property name`() {
        val builder = KeyBuilder<OrderLineAdded>()
        builder.usingKeyWithPropertyName("orderId")
        assertEquals(ResolvedKey.Property("orderId"), builder.build())
    }

    @Test
    fun `usingKeyFromContext resolves to the given context property`() {
        val builder = KeyBuilder<OrderLineAdded>()
        builder.usingKeyFromContext("EventSourceId")
        assertEquals(ResolvedKey.Context("EventSourceId"), builder.build())
    }

    @Test
    fun `usingCompositeKey resolves to every added property in the order they were added`() {
        val builder = KeyBuilder<OrderLineAdded>()
        builder.usingCompositeKey { composite ->
            composite.add(OrderLineAdded::orderId)
            composite.add(OrderLineAdded::product)
        }
        assertEquals(ResolvedKey.Composite(listOf("orderId", "product")), builder.build())
    }

    @Test
    fun `usingCompositeKey accepts property names for Java callers`() {
        val builder = KeyBuilder<OrderLineAdded>()
        builder.usingCompositeKey { composite ->
            composite.addWithPropertyName("orderId")
            composite.addWithPropertyName("product")
        }
        assertEquals(ResolvedKey.Composite(listOf("orderId", "product")), builder.build())
    }

    @Test
    fun `calling a method again overrides the previously resolved key`() {
        val builder = KeyBuilder<OrderLineAdded>()
        builder.usingKey(OrderLineAdded::orderId)
        builder.usingKeyFromContext("CorrelationId")
        assertEquals(ResolvedKey.Context("CorrelationId"), builder.build())
    }
}
