// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import com.google.gson.JsonObject
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.keys.Key
import io.cratis.chronicle.projections.InvalidPropertyForType
import io.cratis.chronicle.projections.UnknownReadModelProperty
import io.cratis.chronicle.readModels.ReadModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@EventType
private data class OrderPlaced(val orderId: String, val customerId: String, val total: Double, val note: String?)

@EventType
private data class OrderCancelled(val orderId: String, val customerId: String, val reason: String)

@EventType
private data class OrderRefunded(val orderId: String, val amount: Double)

private data class Order(@Key val orderId: String, val total: Double, val refunded: Double, val note: String?, val state: String)

private data class Customer(@Key val customerId: String, val name: String)

@Edge("PLACED")
private data class Placed(
    @EdgeSource(Customer::class) val customerId: String,
    @EdgeTarget(Order::class) val orderId: String,
    val total: Double
)

private data class Link(
    @EdgeSource(Customer::class) val customerId: String,
    @EdgeTarget(Order::class) val orderId: String
)

private data class Ticketed(
    @EdgeSource(Customer::class) val customerId: String,
    @EdgeTarget(Order::class) val orderId: String,
    @Key val ticket: String
)

private data class NotAnEdge(val customerId: String, val orderId: String)

@ReadModel
private data class OrderThatIsAReadModel(@Key val orderId: String)

private class Shop(private val define: (IGraphProjectionBuilder) -> Unit) : IGraphProjection {
    override fun define(builder: IGraphProjectionBuilder) = define.invoke(builder)
}

class GraphProjectionBuilderTests {
    private fun build(define: (IGraphProjectionBuilder) -> Unit): JsonObject =
        GraphSnapshot.of(GraphProjections("Neo4j").registrationsFor(listOf(Shop(define))).single())
            .getAsJsonObject("projection").getAsJsonObject("graph")

    private fun JsonObject.obj(name: String): JsonObject = getAsJsonObject(name)

    private fun orderMapping(configure: (IGraphNodeFromBuilder<Order, OrderPlaced>) -> Unit): JsonObject =
        build { it.node(Order::class) { node -> node.from(OrderPlaced::class, configure) } }
            .obj("nodes").obj("Order").obj("from").obj("OrderPlaced+1")

    @Test
    fun `names the graph after the package of the builder`() {
        val registration = GraphProjections("Neo4j").registrationsFor(listOf(Shop {})).single()
        assertEquals("io.cratis.chronicle.projections.graphs", registration.graphName)
        assertEquals(Shop::class.qualifiedName, registration.projection.identifier)
        assertEquals(Shop::class.qualifiedName, registration.readModel.observerIdentifier)
    }

    @Test
    fun `identifies a node by the property it names`() {
        val registration = GraphProjections("Neo4j").registrationsFor(listOf(Shop { builder ->
            builder.node(Customer::class) { it.identifiedBy("name").from(OrderPlaced::class) }
        })).single()
        val customer = com.google.gson.JsonParser.parseString(registration.readModel.schema).asJsonObject
            .obj("properties").obj("Customer")
        assertEquals("name", customer.get("x-graph-key").asString)
    }

    @Test
    fun `rejects a node without a key`() {
        assertThrows(NodeMustHaveExactlyOneKey::class.java) { build { it.node(NotAnEdge::class) { } } }
    }

    @Test
    fun `rejects a node that is also a read model`() {
        assertThrows(GraphElementCannotBeReadModel::class.java) { build { it.node(OrderThatIsAReadModel::class) { } } }
    }

    @Test
    fun `keys a node by the event source id unless told otherwise`() {
        assertEquals("\$eventSourceId", orderMapping { }.get("key").asString)
        assertEquals("customerId", orderMapping { it.usingKey("customerId") }.get("key").asString)
    }

    @Test
    fun `maps properties by name when the mapping says nothing`() {
        val properties = orderMapping { }.obj("properties")
        assertEquals("total", properties.get("total").asString)
        assertEquals("note", properties.get("note").asString)
        assertFalse(properties.has("orderId"))
        assertFalse(properties.has("refunded"))
    }

    @Test
    fun `only changes a node that exists when asked to`() {
        assertFalse(orderMapping { }.get("updateOnly").asBoolean)
        assertTrue(orderMapping { it.onlyWhenExisting() }.get("updateOnly").asBoolean)
    }

    @Test
    fun `sets a property to a value of the event, the event context, the event source id or a constant`() {
        val properties = orderMapping {
            it.set(Order::total).toProperty("total")
            it.set("refunded").toEventContextProperty("occurred")
            it.set(Order::note).toValue(null)
            it.set(Order::state).toValue("placed")
            it.set("state").toEventSourceId()
        }.obj("properties")
        assertEquals("total", properties.get("total").asString)
        assertEquals("\$eventContext(occurred)", properties.get("refunded").asString)
        assertEquals("\$null", properties.get("note").asString)
        assertEquals("\$eventSourceId", properties.get("state").asString)
    }

    @Test
    fun `maps a constant to a property`() {
        assertEquals("\$value(placed)", orderMapping { it.set(Order::state).toValue("placed") }.obj("properties").get("state").asString)
    }

    @Test
    fun `adds, subtracts, increments, decrements and counts`() {
        val properties = build { builder ->
            builder.node(Order::class) { node ->
                node.from(OrderRefunded::class) {
                    it.add(Order::refunded, "amount").subtract("total", "amount")
                        .increment(Order::state).decrement("note").count(Order::orderId)
                }
            }
        }.obj("nodes").obj("Order").obj("from").obj("OrderRefunded+1").obj("properties")
        assertEquals("\$add(amount)", properties.get("refunded").asString)
        assertEquals("\$subtract(amount)", properties.get("total").asString)
        assertEquals("\$increment", properties.get("state").asString)
        assertEquals("\$decrement", properties.get("note").asString)
        assertEquals("\$count", properties.get("orderId").asString)
    }

    @Test
    fun `deletes a node and detaches its edges when asked to`() {
        val nodes = build {
            it.node(Order::class) { node ->
                node.removedWith(OrderCancelled::class) { removal -> removal.usingKey("orderId").detach() }
                    .removedWith(OrderRefunded::class)
            }
        }.obj("nodes").obj("Order").obj("removedWith")
        assertEquals("orderId", nodes.obj("OrderCancelled+1").get("key").asString)
        assertTrue(nodes.obj("OrderCancelled+1").get("detach").asBoolean)
        assertEquals("\$eventSourceId", nodes.obj("OrderRefunded+1").get("key").asString)
        assertFalse(nodes.obj("OrderRefunded+1").get("detach").asBoolean)
    }

    @Test
    fun `takes the type of an edge from the annotation, the builder or the class name`() {
        val edges = build {
            it.edge(Placed::class, Customer::class, Order::class) { edge -> edge.from(OrderPlaced::class) { } }
                .edge(Link::class, Customer::class, Order::class) { edge ->
                    edge.named("LINKED_TO").from(OrderPlaced::class) { }
                }
                .edge(Ticketed::class, Customer::class, Order::class) { edge ->
                    edge.identifiedBy(Ticketed::ticket).from(OrderPlaced::class) { it.usingIdentity("orderId") }
                }
        }.obj("edges")
        assertEquals(setOf("PLACED", "LINKED_TO", "Ticketed"), edges.keySet())
    }

    @Test
    fun `describes the end nodes of an edge by the nodes it is declared between`() {
        val registration = GraphProjections("Neo4j").registrationsFor(listOf(Shop {
            it.edge(Placed::class, Customer::class, Order::class) { edge -> edge.from(OrderPlaced::class) { } }
        })).single()
        val placed = com.google.gson.JsonParser.parseString(registration.readModel.schema).asJsonObject
            .obj("properties").obj("PLACED")
        assertEquals("Customer", placed.get("x-graph-source").asString)
        assertEquals("Order", placed.get("x-graph-target").asString)
        assertEquals(setOf("total"), placed.obj("properties").keySet())
    }

    @Test
    fun `resolves the keys and the properties of an edge`() {
        val from = build {
            it.edge(Placed::class, Customer::class, Order::class) { edge ->
                edge.from(OrderPlaced::class) { mapping ->
                    mapping.usingSourceKey("customerId").usingTargetKey("orderId").set(Placed::total).toProperty("total")
                }.removedWith(OrderCancelled::class) { removal -> removal.usingSourceKey("customerId").usingTargetKey("orderId") }
            }
        }.obj("edges").obj("PLACED")
        assertEquals("customerId", from.obj("from").obj("OrderPlaced+1").get("sourceKey").asString)
        assertEquals("orderId", from.obj("from").obj("OrderPlaced+1").get("targetKey").asString)
        assertEquals("total", from.obj("from").obj("OrderPlaced+1").obj("properties").get("total").asString)
        assertEquals("orderId", from.obj("removedWith").obj("OrderCancelled+1").get("targetKey").asString)
    }

    @Test
    fun `identifies an edge by the event property named like its identifying property`() {
        val edge = build {
            it.edge(Ticketed::class, Customer::class, Order::class) { edge ->
                edge.from(OrderPlaced::class) { mapping -> mapping.usingIdentity("orderId") }
                    .removedWith(OrderCancelled::class) { removal -> removal.usingIdentity("reason") }
            }
        }.obj("edges").obj("Ticketed")
        assertEquals("orderId", edge.obj("from").obj("OrderPlaced+1").get("identity").asString)
        assertEquals("reason", edge.obj("removedWith").obj("OrderCancelled+1").get("identity").asString)
    }

    @Test
    fun `rejects an identified edge when the event does not say where the identity is`() {
        assertThrows(UnknownGraphEventProperty::class.java) {
            build { it.edge(Ticketed::class, Customer::class, Order::class) { edge -> edge.from(OrderPlaced::class) { } } }
        }
    }

    @Test
    fun `rejects an edge without a source and a target`() {
        assertThrows(EdgeMustHaveOneSourceAndOneTarget::class.java) {
            build { it.edge(NotAnEdge::class, Customer::class, Order::class) { } }
        }
    }

    @Test
    fun `rejects a property that the node does not have`() {
        assertThrows(UnknownReadModelProperty::class.java) { orderMapping { it.set("missing") } }
        assertThrows(UnknownReadModelProperty::class.java) { orderMapping { it.increment("missing") } }
        assertThrows(UnknownReadModelProperty::class.java) { build { it.node(Order::class) { node -> node.identifiedBy("missing") } } }
    }

    @Test
    fun `rejects an event property that the event does not have`() {
        assertThrows(InvalidPropertyForType::class.java) { orderMapping { it.usingKey("missing") } }
        assertThrows(InvalidPropertyForType::class.java) { orderMapping { it.set(Order::total).toProperty("missing") } }
        assertThrows(InvalidPropertyForType::class.java) { orderMapping { it.set(Order::total).toEventContextProperty("occured") } }
        assertThrows(InvalidPropertyForType::class.java) { orderMapping { it.add(Order::total, "missing") } }
    }

    @Test
    fun `rejects an event that is not an event type`() {
        assertThrows(IllegalArgumentException::class.java) {
            build { it.node(Order::class) { node -> node.from(Shop::class) } }
        }
    }
}
