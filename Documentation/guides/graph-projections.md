---
title: Graph projections
description: Project events onto a property graph of nodes, edges and properties stored in Neo4j, declared with annotations or a fluent builder in Kotlin and Java.
---

A graph projection projects events onto a **property graph**: nodes, edges and
their properties. It works like a projection onto a read model - the same
observer, event routing, key and property expressions, replay and failed
partition handling - but its target is a graph. Instead of folding events into
one document per key, the kernel turns each event into a few idempotent
operations on nodes and edges, and a graph sink such as Neo4j applies them.

The client only declares the graph. It builds the same projection and read
model definitions the .NET client builds and registers them with the kernel,
which does all the work. The shared pages explain the model:

- [Graph projections](/chronicle/projections/graph-projections/)
- [Sinks](/chronicle/sinks/)

## Nodes and edges

A **node** has a label - the simple name of its class - a unique key and
properties. An **edge** connects two nodes. It has a type such as `MEMBER_OF`,
a source node, a target node and properties. By default an edge is identified
by its type, source and target, so there is at most one edge of a type between
two nodes. An edge can name an identifying property, which allows several edges
of the same type between the same two nodes.

A property is a primitive or a collection of primitives. An object property is
an error: model the object as a node of its own and connect it with an edge.

## Declare a graph with annotations

Annotate the node and edge classes. Everything that belongs to one graph shares
the name from `@Graph`; without it a graph is named after the package of its
types.

| Annotation | On | Use it for |
| --- | --- | --- |
| `@Graph` | node, edge, `IGraphProjection` | The name of the graph. |
| `@Node` | class | A node type. Needs exactly one `@Key` property. |
| `@Edge` | class | An edge type, such as `MEMBER_OF`. |
| `@Key` | property | The unique key of a node, or the identity of an edge. |
| `@EdgeSource`, `@EdgeTarget` | edge property | Hold the end node keys. |
| `@FromEvent` | node | The event that creates or updates the node. |
| `@RemovedWith` | node | Deletes the node; `detach = true` drops its edges. |
| `@EdgeFrom` | edge | The event that creates or updates the edge. |
| `@EdgeRemovedWith` | edge | The event that deletes the edge. The nodes stay. |

Properties map by name from an event property of the same name, or explicitly
with the annotations a read model already uses: `@SetFrom`, `@SetFromContext`,
`@SetValue`, `@Increment`, `@Decrement`, `@Count`, `@AddFrom` and
`@SubtractFrom`. A node is keyed by the event source id unless `@FromEvent`
names an event property in `key`.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.keys.Key
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.RemovedWith
import io.cratis.chronicle.projections.SetFromContext
import io.cratis.chronicle.projections.graphs.Edge
import io.cratis.chronicle.projections.graphs.EdgeFrom
import io.cratis.chronicle.projections.graphs.EdgeRemovedWith
import io.cratis.chronicle.projections.graphs.EdgeSource
import io.cratis.chronicle.projections.graphs.EdgeTarget
import io.cratis.chronicle.projections.graphs.Graph
import io.cratis.chronicle.projections.graphs.Node
import java.time.Instant

@EventType
data class AgentHired(val agentId: String, val name: String)

@EventType
data class AgentLeft(val agentId: String)

@EventType
data class DeskOpened(val deskId: String, val name: String)

@EventType
data class AgentSeated(val agentId: String, val deskId: String)

@EventType
data class AgentMoved(val agentId: String, val deskId: String)

@Graph("SupportFloor")
@Node
@FromEvent(AgentHired::class)
@RemovedWith(AgentLeft::class, detach = true)
data class Agent(@Key val agentId: String, val name: String)

@Graph("SupportFloor")
@Node
@FromEvent(DeskOpened::class)
data class Desk(@Key val deskId: String, val name: String)

@Graph("SupportFloor")
@Edge("SITS_AT")
@EdgeFrom(AgentSeated::class, sourceKey = "agentId", targetKey = "deskId")
@EdgeRemovedWith(AgentMoved::class, sourceKey = "agentId", targetKey = "deskId")
data class SitsAt(
    @EdgeSource(Agent::class) val agentId: String,
    @EdgeTarget(Desk::class) val deskId: String,
    @SetFromContext(AgentSeated::class, "occurred") val since: Instant
)
```

Java writes the same with records. A Kotlin annotation that has more than one
parameter takes them by name from Java, as the other projection annotations do;
`@Graph`, `@Edge`, `@EdgeSource` and `@EdgeTarget` have a single `value`.

<!-- validate: declarations -->

```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.keys.Key;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.RemovedWith;
import io.cratis.chronicle.projections.graphs.Graph;
import io.cratis.chronicle.projections.graphs.Node;

@EventType
record LibrarianHired(String librarianId, String name) {}

@EventType
record LibrarianLeft(String librarianId) {}

@Graph("Library")
@Node
@FromEvent(eventType = LibrarianHired.class)
@RemovedWith(eventType = LibrarianLeft.class, detach = true)
record Librarian(@Key String librarianId, String name) {}
```

## Declare a graph with a builder

An `IGraphProjection` produces exactly the definition the annotations would.
Element properties are given as `KProperty1` references in Kotlin or by name in
Java, and event properties by name; a name the node or the event does not have
is reported when the builder runs.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.projections.graphs.IGraphProjection
import io.cratis.chronicle.projections.graphs.IGraphProjectionBuilder

@Graph("SupportFloorByBuilder")
class SupportFloorGraph : IGraphProjection {
    override fun define(builder: IGraphProjectionBuilder) {
        builder
            .node(Agent::class) { node ->
                node.identifiedBy(Agent::agentId)
                    .from(AgentHired::class)
                    .removedWith(AgentLeft::class) { it.detach() }
            }
            .node(Desk::class) { node ->
                node.identifiedBy(Desk::deskId).from(DeskOpened::class)
            }
            .edge(SitsAt::class, Agent::class, Desk::class) { edge ->
                edge.from(AgentSeated::class) { from ->
                    from.usingSourceKey("agentId")
                        .usingTargetKey("deskId")
                        .set(SitsAt::since).toEventContextProperty("occurred")
                }
            }
    }
}
```

Counters - `increment`, `decrement` and `count` in the builder, or `@Increment`,
`@Decrement` and `@Count` on a property - work on node and edge properties
alike. An edge identified by a `@Key` property needs every mapping to say which
event property holds the identity, with `identity` on `@EdgeFrom` and
`@EdgeRemovedWith` or `usingIdentity` in the builder; an event with a property
named like the key is enough otherwise.

## Choose the graph sink

A graph is stored by a graph sink, which defaults to `Neo4j`. Set another one
for the whole client with `withDefaultGraphSinkTypeId`; the in-memory graph sink
keeps the graph in the kernel's memory for development and tests.

<!-- validate: body -->

```kotlin
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.sinks.WellKnownSinkTypes

val options = ChronicleOptions.development()
    .withDefaultGraphSinkTypeId(WellKnownSinkTypes.IN_MEMORY_GRAPH)
```

## Registration

Annotated nodes and edges, and `IGraphProjection` implementations, are
discovered and registered with the other artifacts - see
[Artifact registration](artifact-registration.md). To register them by hand,
pass the node and edge classes, or the builder instance, to
`store.projections.register(...)`. The read model the graph is stored as is
registered first, then the projection that fills it.

## What is checked

A declaration that cannot work fails when the artifacts are registered, with an
exception that names the type:

- `NodeMustHaveExactlyOneKey`: a node has no `@Key` property, or more than one.
- `EdgeMustHaveOneSourceAndOneTarget`: an edge lacks the `@EdgeSource` or the
  `@EdgeTarget` property.
- `EdgeCanHaveOnlyOneIdentifyingKey`: an edge has more than one `@Key`
  property.
- `NestedObjectInGraph`: a node or edge has an object, or a collection of
  objects, as a property.
- `UnknownGraphEventProperty`: an identified edge has an event that neither
  names nor has the identity.
- `GraphElementCannotBeReadModel`: a node or edge is also a `@ReadModel`.

## Not in the Kotlin and Java client yet

- An in-process test scenario for graphs. The shared testing example shows that
  Kotlin does not support it yet; the kernel's in-memory graph sink runs a graph
  without a database.
- Naming a sink configuration for a graph, and mapping one operation per item
  of an event's collection (`forEach`). Both need a newer contracts package than
  the client builds against, and the .NET client does not expose `forEach` either.
