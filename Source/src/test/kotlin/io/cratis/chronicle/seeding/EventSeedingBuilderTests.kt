// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.seeding

import io.cratis.chronicle.events.EventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@EventType
private data class ProductRegistered(val name: String)

private data class NotAnEvent(val value: String)

class EventSeedingBuilderTests {

    @Test
    fun `forEventSource adds an entry with no namespace, defaulting to the event store's own`() {
        val builder = EventSeedingBuilder()
        val event = ProductRegistered("Widget")

        builder.forEventSource("product-1", listOf(event))

        val entry = builder.build().single()
        assertEquals("product-1", entry.eventSourceId)
        assertEquals(listOf(event), entry.events)
        assertNull(entry.namespace)
    }

    @Test
    fun `forEventType rejects a class that is not annotated with EventType`() {
        val builder = EventSeedingBuilder()

        assertThrows(IllegalArgumentException::class.java) {
            builder.forEventType(NotAnEvent::class, "source-1", listOf(NotAnEvent("oops")))
        }
    }

    @Test
    fun `forEventType adds an unscoped entry, same as forEventSource`() {
        val builder = EventSeedingBuilder()
        val events = listOf(ProductRegistered("Widget"), ProductRegistered("Gadget"))

        builder.forEventType(ProductRegistered::class, "product-1", events)

        val entry = builder.build().single()
        assertEquals("product-1", entry.eventSourceId)
        assertEquals(events, entry.events)
        assertNull(entry.namespace)
    }

    @Test
    fun `forNamespace scopes entries added through it to that namespace`() {
        val builder = EventSeedingBuilder()

        builder.forEventSource("global-source", listOf(ProductRegistered("Global")))
        builder.forNamespace("tenant-a").forEventSource("product-1", listOf(ProductRegistered("Widget")))

        val entries = builder.build()
        assertEquals(2, entries.size)
        assertNull(entries[0].namespace)
        assertEquals("tenant-a", entries[1].namespace)
    }

    @Test
    fun `forNamespace's forEventType also validates the EventType annotation`() {
        val scopeBuilder = EventSeedingBuilder().forNamespace("tenant-a")

        assertThrows(IllegalArgumentException::class.java) {
            scopeBuilder.forEventType(NotAnEvent::class, "source-1", listOf(NotAnEvent("oops")))
        }
    }

    @Test
    fun `multiple forNamespace calls can target different namespaces from the same builder`() {
        val builder = EventSeedingBuilder()

        builder.forNamespace("tenant-a").forEventSource("product-1", listOf(ProductRegistered("Widget")))
        builder.forNamespace("tenant-b").forEventSource("product-2", listOf(ProductRegistered("Gadget")))

        val namespaces = builder.build().map { it.namespace }
        assertEquals(listOf("tenant-a", "tenant-b"), namespaces)
    }
}
