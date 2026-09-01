// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import io.cratis.chronicle.events.EventType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
private data class OrderPlaced(val orderId: String, val customerId: String)

@EventType
private data class OrderCancelled(val orderId: String)

@EventType
private data class OrderLineAdded(val orderId: String, val product: String, val quantity: Int)

@EventType
private data class OrderLineRemoved(val orderId: String, val product: String)

private data class OrderLine(val product: String, val quantity: Int)

private data class Order(
    val id: String,
    val customerName: String,
    val lines: List<OrderLine> = emptyList(),
    val summary: OrderSummary? = null,
    val version: Int = 0,
    val totalQuantity: Int = 0
)

private data class OrderSummary(val note: String)

class ProjectionBuilderForTests {

    @Test
    fun `join collects the joined-on property and the mapped properties for the event type`() {
        val builder = ProjectionBuilderFor(Order::class)
        builder.join(OrderPlaced::class) { jb ->
            jb.on(Order::id)
            jb.set(Order::customerName).toProperty("customerId")
        }
        assertEquals(1, builder.joinEntries.size)
        val entry = builder.joinEntries.first()
        assertEquals(OrderPlaced::class, entry.eventClass)
        assertEquals("id", entry.on)
        assertEquals(mapOf("customerName" to "customerId"), entry.properties)
    }

    @Test
    fun `fromEvery and fromAll accumulate into the same property map`() {
        val builder = ProjectionBuilderFor(Order::class)
        builder.fromEvery { feb -> feb.set(Order::id).toEventSourceId() }
        builder.fromAll { feb -> feb.set(Order::customerName).toEventContextProperty("CausedBy") }
        assertEquals(
            mapOf("id" to "\$eventSourceId", "customerName" to "\$eventContext(CausedBy)"),
            builder.fromEveryProperties
        )
    }

    @Test
    fun `removedWith defaults key and parentKey to EventSourceId`() {
        val builder = ProjectionBuilderFor(Order::class)
        builder.removedWith(OrderCancelled::class)
        val entry = builder.removedWithEntries.single()
        assertEquals("EventSourceId", entry.key)
        assertEquals("EventSourceId", entry.parentKey)
    }

    @Test
    fun `removedWith honors explicit key configuration`() {
        val builder = ProjectionBuilderFor(Order::class)
        builder.removedWith(OrderCancelled::class) { rb -> rb.usingKey("orderId") }
        assertEquals("orderId", builder.removedWithEntries.single().key)
    }

    @Test
    fun `removedWithJoin collects the configured key`() {
        val builder = ProjectionBuilderFor(Order::class)
        builder.removedWithJoin(OrderCancelled::class) { rb -> rb.usingKey("orderId") }
        assertEquals("orderId", builder.removedWithJoinEntries.single().key)
    }

    @Test
    fun `children collects identifiedBy and from entries with per-event key and parentKey`() {
        val builder = ProjectionBuilderFor(Order::class)
        builder.children(Order::lines, OrderLine::class) { cb ->
            cb.identifiedBy("product")
            cb.from(OrderLineAdded::class) { fb ->
                fb.usingKey("product")
                fb.usingParentKey("orderId")
                fb.set(OrderLine::quantity).toProperty("quantity")
            }
        }
        val entry = builder.childrenEntries.single()
        assertEquals("lines", entry.propertyName)
        assertEquals("product", entry.identifiedBy)
        val fromEntry = entry.fromEntries.single()
        assertEquals(OrderLineAdded::class, fromEntry.eventClass)
        assertEquals("product", fromEntry.key)
        assertEquals("orderId", fromEntry.parentKey)
        assertEquals(mapOf("quantity" to "quantity"), fromEntry.properties)
    }

    @Test
    fun `nested collects from entries and clearWith event classes`() {
        val builder = ProjectionBuilderFor(Order::class)
        builder.nested(Order::summary, OrderSummary::class) { nb ->
            nb.from(OrderPlaced::class) { fb -> fb.set(OrderSummary::note).toProperty("orderId") }
            nb.clearWith(OrderCancelled::class)
        }
        val entry = builder.nestedEntries.single()
        assertEquals("summary", entry.propertyName)
        assertEquals(1, entry.fromEntries.size)
        assertEquals(listOf(OrderCancelled::class), entry.clearWithEventClasses)
    }

    @Test
    fun `usingCompositeKey builds a dollar-composite expression from its parts`() {
        val builder = ProjectionBuilderFor(Order::class)
        builder.from(OrderLineAdded::class) { fb ->
            fb.usingCompositeKey { ck -> ck.property("orderId", "orderId").property("product", "product") }
        }
        assertEquals("\$composite(orderId=orderId,product=product)", builder.fromEntries.single().key)
    }

    @Test
    fun `usingConstantKey wraps the value as a dollar-value expression`() {
        val builder = ProjectionBuilderFor(Order::class)
        builder.from(OrderPlaced::class) { fb -> fb.usingConstantKey("singleton") }
        assertEquals("\$value(singleton)", builder.fromEntries.single().key)
    }

    @Test
    fun `notRewindable flips isRewindable off`() {
        val builder = ProjectionBuilderFor(Order::class)
        assertTrue(builder.isRewindable)
        builder.notRewindable()
        assertFalse(builder.isRewindable)
    }

    @Test
    fun `noAutoMap and autoMap toggle autoMapEnabled`() {
        val builder = ProjectionBuilderFor(Order::class)
        assertTrue(builder.autoMapEnabled)
        builder.noAutoMap()
        assertFalse(builder.autoMapEnabled)
        builder.autoMap()
        assertTrue(builder.autoMapEnabled)
    }

    @Test
    fun `set by property name produces the same mapping as the KProperty1 form`() {
        val byProperty = ProjectionBuilderFor(Order::class)
        byProperty.from(OrderPlaced::class) { it.set(Order::customerName).toProperty("customerId") }

        val byName = ProjectionBuilderFor(Order::class)
        byName.from(OrderPlaced::class) { it.set<String>("customerName").toProperty("customerId") }

        assertEquals(byProperty.fromEntries.single().properties, byName.fromEntries.single().properties)
    }

    @Test
    fun `on by property name produces the same join entry as the KProperty1 form`() {
        val byProperty = ProjectionBuilderFor(Order::class)
        byProperty.join(OrderPlaced::class) { it.on(Order::id) }

        val byName = ProjectionBuilderFor(Order::class)
        byName.join(OrderPlaced::class) { it.on("id") }

        assertEquals(byProperty.joinEntries.single().on, byName.joinEntries.single().on)
    }

    @Test
    fun `children by property name and Class produces the same entry as the KProperty1 and KClass form`() {
        val byProperty = ProjectionBuilderFor(Order::class)
        byProperty.children(Order::lines, OrderLine::class) { it.identifiedBy("product") }

        val byName = ProjectionBuilderFor(Order::class)
        byName.children("lines", OrderLine::class.java) { it.identifiedBy("product") }

        assertEquals(byProperty.childrenEntries.single().propertyName, byName.childrenEntries.single().propertyName)
        assertEquals(byProperty.childrenEntries.single().identifiedBy, byName.childrenEntries.single().identifiedBy)
    }

    @Test
    fun `nested by property name and Class produces the same entry as the KProperty1 and KClass form`() {
        val byProperty = ProjectionBuilderFor(Order::class)
        byProperty.nested(Order::summary, OrderSummary::class) { it.clearWith(OrderCancelled::class) }

        val byName = ProjectionBuilderFor(Order::class)
        byName.nested("summary", OrderSummary::class.java) { it.clearWith(OrderCancelled::class.java) }

        assertEquals(byProperty.nestedEntries.single().propertyName, byName.nestedEntries.single().propertyName)
        assertEquals(byProperty.nestedEntries.single().clearWithEventClasses, byName.nestedEntries.single().clearWithEventClasses)
    }

    @Test
    fun `count records the well-known dollar-count expression`() {
        val builder = ProjectionBuilderFor(Order::class)
        builder.from(OrderPlaced::class) { it.count("version") }
        assertEquals(mapOf("version" to "\$count"), builder.fromEntries.single().properties)
    }

    @Test
    fun `increment and decrement record their well-known expressions`() {
        val incrementing = ProjectionBuilderFor(Order::class)
        incrementing.from(OrderLineAdded::class) { it.increment(Order::version) }
        assertEquals(mapOf("version" to "\$increment"), incrementing.fromEntries.single().properties)

        val decrementing = ProjectionBuilderFor(Order::class)
        decrementing.from(OrderLineRemoved::class) { it.decrement("version") }
        assertEquals(mapOf("version" to "\$decrement"), decrementing.fromEntries.single().properties)
    }

    @Test
    fun `add and subtract record the source event property in their well-known expressions`() {
        val adding = ProjectionBuilderFor(Order::class)
        adding.from(OrderLineAdded::class) { it.add(Order::totalQuantity).with("quantity") }
        assertEquals(mapOf("totalQuantity" to "\$add(quantity)"), adding.fromEntries.single().properties)

        val subtracting = ProjectionBuilderFor(Order::class)
        subtracting.from(OrderLineRemoved::class) { it.subtract("totalQuantity").with("quantity") }
        assertEquals(mapOf("totalQuantity" to "\$subtract(quantity)"), subtracting.fromEntries.single().properties)
    }

    @Test
    fun `toEventContextProperty on from maps to the well-known dollar-eventContext expression`() {
        val builder = ProjectionBuilderFor(Order::class)
        builder.from(OrderPlaced::class) { it.set(Order::customerName).toEventContextProperty("CausedBy") }
        assertEquals(mapOf("customerName" to "\$eventContext(CausedBy)"), builder.fromEntries.single().properties)
    }

    @Test
    fun `an unknown property name fails clearly instead of emitting a bad definition`() {
        val builder = ProjectionBuilderFor(Order::class)
        assertThrows(UnknownReadModelProperty::class.java) {
            builder.from(OrderPlaced::class) { it.set<String>("doesNotExist").toProperty("customerId") }
        }
    }
}
