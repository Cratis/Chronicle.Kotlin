// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import com.google.gson.JsonObject
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.keys.Key
import io.cratis.chronicle.projections.AddFrom
import io.cratis.chronicle.projections.Count
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.InvalidPropertyForType
import io.cratis.chronicle.projections.RemovedWith
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.SetFromContext
import io.cratis.chronicle.projections.SetValue
import io.cratis.chronicle.projections.SubtractFrom
import io.cratis.chronicle.readModels.ReadModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.reflect.KClass

@EventType
private data class WidgetStocked(
    val widgetId: String,
    val label: String,
    val quantity: Int,
    val tags: List<String>,
    val specification: WidgetSpecification
)

@EventType
private data class WidgetSold(val widgetId: String, val quantity: Int)

@EventType
private data class WidgetScrapped(val widgetId: String)

@EventType
private data class WidgetReserved(val widgetId: String, val customerId: String)

private data class WidgetSpecification(val weight: Double, val color: String)

private class NotAnEvent

@Graph("Warehouse")
@Node
@FromEvent(WidgetStocked::class, key = "widgetId")
@RemovedWith(WidgetScrapped::class, key = "widgetId")
private data class Widget(@Key val widgetId: String, val label: String, val tags: List<String>)

@Graph("Warehouse")
@Node
private data class WidgetWithoutKey(val widgetId: String)

@Graph("Warehouse")
@Node
private data class WidgetWithTwoKeys(@Key val widgetId: String, @Key val label: String)

@Graph("Warehouse")
@Node
@FromEvent(WidgetStocked::class)
private data class WidgetWithObject(@Key val widgetId: String, val specification: WidgetSpecification)

@Graph("Warehouse")
@Node
private data class WidgetWithObjects(@Key val widgetId: String, val specifications: List<WidgetSpecification>)

@Graph("Warehouse")
@Node
@ReadModel
private data class WidgetThatIsAlsoAReadModel(@Key val widgetId: String)

@Graph("Warehouse")
@Node
@FromEvent(WidgetStocked::class, key = "widgtId")
private data class WidgetKeyedByATypo(@Key val widgetId: String)

@Graph("Warehouse")
@Node
@FromEvent(NotAnEvent::class)
private data class WidgetFromANonEvent(@Key val widgetId: String)

@Graph("Warehouse")
@Node
@FromEvent(WidgetStocked::class, key = "widgetId")
private data class WidgetNamingAnObject(@Key val widgetId: String, val specification: String)

@Graph("Warehouse")
@Node
@FromEvent(WidgetStocked::class)
private data class WidgetWithMappings(
    @Key val widgetId: String,
    @SetFrom("label", WidgetStocked::class) val title: String,
    @SetFromContext(WidgetStocked::class, "occurred") val stockedAt: Instant,
    @SetValue(WidgetStocked::class, "stocked") val state: String,
    @AddFrom(WidgetSold::class, "quantity") @SubtractFrom(WidgetStocked::class, "quantity") val balance: Int,
    @Count(WidgetReserved::class) val reservations: Int
)

@Graph("Warehouse")
@Node
private data class ShelfWithoutEvents(@Key val shelfId: String)

@Graph("Warehouse")
@Edge("STORED_ON")
@EdgeFrom(WidgetStocked::class, sourceKey = "widgetId", targetKey = "label")
private data class StoredOn(
    @EdgeSource(Widget::class) val widgetId: String,
    @EdgeTarget(ShelfWithoutEvents::class) val shelfId: String
)

@Graph("Warehouse")
@Edge("NO_TARGET")
private data class EdgeWithoutTarget(@EdgeSource(Widget::class) val widgetId: String)

@Graph("Warehouse")
@Edge("TWO_KEYS")
private data class EdgeWithTwoKeys(
    @EdgeSource(Widget::class) val widgetId: String,
    @EdgeTarget(ShelfWithoutEvents::class) val shelfId: String,
    @Key val first: String,
    @Key val second: String
)

@Graph("Warehouse")
@Edge("RESERVED_BY")
@EdgeFrom(WidgetReserved::class, sourceKey = "widgetId", targetKey = "customerId")
private data class ReservedByWithoutIdentity(
    @EdgeSource(Widget::class) val widgetId: String,
    @EdgeTarget(ShelfWithoutEvents::class) val customerId: String,
    @Key val reservationId: String
)

@Graph("Warehouse")
@Edge("RESERVED_BY")
@EdgeFrom(WidgetReserved::class, sourceKey = "widgetId", targetKey = "customerId", identity = "widgetId")
private data class ReservedByWithIdentity(
    @EdgeSource(Widget::class) val widgetId: String,
    @EdgeTarget(ShelfWithoutEvents::class) val customerId: String,
    @Key val reservationId: String
)

@Node
@FromEvent(WidgetStocked::class)
private data class WidgetWithoutAGraphName(@Key val widgetId: String)

class ModelBoundGraphDeclarationTests {
    private fun register(vararg types: KClass<*>) = GraphProjections("Neo4j").registrationsFor(types.toList())

    private fun JsonObject.obj(name: String): JsonObject = getAsJsonObject(name)

    private fun nodesOf(vararg types: KClass<*>): JsonObject =
        GraphSnapshot.of(register(*types).single()).obj("projection").obj("graph").obj("nodes")

    @Test
    fun `groups nodes and edges that share a graph name into one graph`() {
        val registration = register(Widget::class, ShelfWithoutEvents::class, StoredOn::class).single()
        assertEquals("Warehouse", registration.graphName)
        assertEquals(setOf("Widget", "ShelfWithoutEvents"), registration.projection.graph.nodesMap.keys)
        assertEquals(setOf("STORED_ON"), registration.projection.graph.edgesMap.keys)
    }

    @Test
    fun `names a graph after the package of its types when it has no name`() {
        val registration = register(WidgetWithoutAGraphName::class).single()
        assertEquals("io.cratis.chronicle.projections.graphs", registration.graphName)
        assertEquals(registration.graphName, registration.projection.identifier)
    }

    @Test
    fun `leaves everything that is not a graph alone`() {
        assertTrue(register(NotAnEvent::class, WidgetStocked::class).isEmpty())
    }

    @Test
    fun `rejects a node without a key`() {
        val error = assertThrows(NodeMustHaveExactlyOneKey::class.java) { register(WidgetWithoutKey::class) }
        assertTrue(error.message!!.contains("WidgetWithoutKey"))
        assertTrue(error.message!!.contains("but it has 0"))
    }

    @Test
    fun `rejects a node with two keys`() {
        val error = assertThrows(NodeMustHaveExactlyOneKey::class.java) { register(WidgetWithTwoKeys::class) }
        assertTrue(error.message!!.contains("but it has 2"))
    }

    @Test
    fun `rejects an edge without a target`() {
        assertThrows(EdgeMustHaveOneSourceAndOneTarget::class.java) { register(EdgeWithoutTarget::class) }
    }

    @Test
    fun `rejects an edge with two keys`() {
        assertThrows(EdgeCanHaveOnlyOneIdentifyingKey::class.java) { register(EdgeWithTwoKeys::class) }
    }

    @Test
    fun `rejects a node with an object property`() {
        val error = assertThrows(NestedObjectInGraph::class.java) { register(WidgetWithObject::class) }
        assertTrue(error.message!!.contains("'specification'"))
    }

    @Test
    fun `rejects a node with a collection of objects`() {
        val error = assertThrows(NestedObjectInGraph::class.java) { register(WidgetWithObjects::class) }
        assertTrue(error.message!!.contains("'specifications'"))
    }

    @Test
    fun `rejects a node that is also a read model`() {
        assertThrows(GraphElementCannotBeReadModel::class.java) { register(WidgetThatIsAlsoAReadModel::class) }
    }

    @Test
    fun `rejects a key that is not a property of the event`() {
        assertThrows(InvalidPropertyForType::class.java) { register(WidgetKeyedByATypo::class) }
    }

    @Test
    fun `rejects an event that is not an event type`() {
        val error = assertThrows(IllegalArgumentException::class.java) { register(WidgetFromANonEvent::class) }
        assertTrue(error.message!!.contains("is not an event type"))
    }

    @Test
    fun `rejects an identified edge when the event does not say where the identity is`() {
        val error = assertThrows(UnknownGraphEventProperty::class.java) { register(ReservedByWithoutIdentity::class) }
        assertTrue(error.message!!.contains("'reservationId'"))
    }

    @Test
    fun `takes the identity of an edge from the event property the annotation names`() {
        val registration = register(ReservedByWithIdentity::class).single()
        assertEquals(
            "widgetId",
            registration.projection.graph.edgesMap.getValue("RESERVED_BY").fromMap.getValue("WidgetReserved+1").identity
        )
    }

    @Test
    fun `does not map an event property that is an object`() {
        val properties = nodesOf(WidgetNamingAnObject::class).obj("WidgetNamingAnObject").obj("from").obj("WidgetStocked+1").obj("properties")
        assertFalse(properties.has("specification"))
        assertEquals(
            setOf("label", "tags"),
            nodesOf(Widget::class).obj("Widget").obj("from").obj("WidgetStocked+1").obj("properties").keySet()
        )
    }

    @Test
    fun `maps a property that an annotation names explicitly`() {
        val from = nodesOf(WidgetWithMappings::class).obj("WidgetWithMappings").obj("from")
        val stocked = from.obj("WidgetStocked+1").obj("properties")
        assertEquals("label", stocked.get("title").asString)
        assertEquals("\$eventContext(occurred)", stocked.get("stockedAt").asString)
        assertEquals("\$value(stocked)", stocked.get("state").asString)
        assertEquals("\$subtract(quantity)", stocked.get("balance").asString)
    }

    @Test
    fun `projects an event that only a property refers to, keyed by the event source id`() {
        val from = nodesOf(WidgetWithMappings::class).obj("WidgetWithMappings").obj("from")
        assertEquals(setOf("WidgetStocked+1", "WidgetSold+1", "WidgetReserved+1"), from.keySet())
        assertEquals("\$eventSourceId", from.obj("WidgetSold+1").get("key").asString)
        assertEquals("\$add(quantity)", from.obj("WidgetSold+1").obj("properties").get("balance").asString)
        assertEquals("\$count", from.obj("WidgetReserved+1").obj("properties").get("reservations").asString)
    }

    @Test
    fun `keys an edge by the event source id unless it says otherwise`() {
        val from = GraphSnapshot.of(register(Widget::class, ShelfWithoutEvents::class, StoredOn::class).single())
            .obj("projection").obj("graph").obj("edges").obj("STORED_ON").obj("from").obj("WidgetStocked+1")
        assertEquals("widgetId", from.get("sourceKey").asString)
        assertEquals("label", from.get("targetKey").asString)
    }

    @Test
    fun `removes a node by the event property the annotation names`() {
        val removal = nodesOf(Widget::class).obj("Widget").obj("removedWith").obj("WidgetScrapped+1")
        assertEquals("widgetId", removal.get("key").asString)
        assertFalse(removal.get("detach").asBoolean)
    }
}
