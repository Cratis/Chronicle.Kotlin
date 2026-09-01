// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.keys

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

private data class KeyTestsOrderLineAdded(@Key val orderId: String, val product: String)

private class KeyTestsOrderLineHandlers {
    @ContextKey("EventSourceId")
    fun orderLineAdded(event: KeyTestsOrderLineAdded) = Unit
}

class KeyTests {

    @Test
    fun `a property annotated with Key is discoverable by reflection`() {
        val property = KeyTestsOrderLineAdded::class.memberProperties.first { it.name == "orderId" }
        assertNotNull(property.findAnnotation<Key>())
    }

    @Test
    fun `a property not annotated with Key carries no annotation`() {
        val property = KeyTestsOrderLineAdded::class.memberProperties.first { it.name == "product" }
        assertNull(property.findAnnotation<Key>())
    }

    @Test
    fun `a function annotated with ContextKey exposes the property it names`() {
        val function = KeyTestsOrderLineHandlers::class.members.first { it.name == "orderLineAdded" }
        val annotation = function.findAnnotation<ContextKey>()
        assertNotNull(annotation)
        assertEquals("EventSourceId", annotation!!.property)
    }
}
