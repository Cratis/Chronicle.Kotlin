---
title: Graph projections
description: Find the shared guide to graph projections that project events onto a property graph of nodes and edges, with Kotlin and Java tabs for the JVM client.
sharedTopicBridge: true
---

Graph projections are documented in the shared Chronicle docs with
synchronized examples for C#, Kotlin and Java: nodes and edges declared with
annotations or the fluent `IGraphProjection` builder, counters, edges with an
identity, and choosing the graph sink.

- [Graph projections](/chronicle/projections/graph-projections/)
- [Sinks](/chronicle/sinks/)

Use the [Kotlin annotation reference](../reference/annotations.md#graph) for
`@Graph`, `@Node`, `@Edge`, `@EdgeSource`, `@EdgeTarget`, `@EdgeFrom` and
`@EdgeRemovedWith`, and for views `@ViewOf`, `@Follows` and `@FromEdge`. Annotated nodes and edges, and `IGraphProjection`
implementations, are registered with the other artifacts; see
[Artifact registration](artifact-registration.md). To register them by hand,
pass the node and edge classes, or the builder instance, to
`store.projections.register(...)`. The read model the graph is stored as is
registered first, then the projection that fills it.

## What the JVM client checks

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

## Read a graph with views

A graph view is a read model over a graph that already exists. Annotate a class
with `@ViewOf` to anchor it at one node. Its constructor properties are its
members, in declaration order, and each is exactly one of:

- A property of the node. It is matched by name, ignoring case. The property
  marked `@Key` is the key of the node, whatever the view calls it.
- A relation, marked `@Follows` with the edge class. The type of the property
  gives the target view and the cardinality: a collection is many, a single
  nullable value is one. The direction follows from the labels of the edge. An
  edge that starts and ends at the same node label needs
  `direction = FollowDirection.Outgoing` or `FollowDirection.Incoming`.
- A property of the edge that was followed, marked `@FromEdge` (with an
  optional `source` when the name differs). It is only valid in a view that is
  the target of relations of one edge type.

A view only declares what to read. It cannot also be a node or an edge, and a
class annotated with `@ViewOf` is discovered like the other graph artifacts
(`IClientArtifacts.graphViews`).

`GraphViews.definitionsFor(...)` checks the views against the graph and returns
one `GraphViewDefinition` per graph that has views. `toJson()` gives its
canonical JSON, and `toJson(includeSchema = true)` adds the nodes and edges the
views were checked against. When a view breaks a rule, an exception that extends
`InvalidGraphView` is thrown. Its `reason` is the stable name of the rule, the
same in every Chronicle client:

- `ViewRootNotANode`: the view is anchored at a class that is not a node of a
  declared graph.
- `ViewIsAlsoSchema`: the view class is also a node or an edge.
- `DuplicateViewName`: two views of a graph have the same name.
- `UnknownNodeProperty`: a property member is not a property of the node.
- `UnknownEdge`: a relation follows a class that is not an edge of the graph.
- `UnknownEdgeProperty`: an edge property member is not a property of the edge.
- `EdgeNotIncidentOnRoot`: the edge does not start or end at the node the view
  is anchored at, or an explicit direction contradicts the edge.
- `AmbiguousDirection`: a self edge is followed without a direction.
- `TargetLabelMismatch`: the target view is anchored at the wrong node for the
  far end of the edge.
- `RelationTargetNotAView`: the type of a relation is not a declared view.
- `RecursiveView`: a view contains itself, directly or through other views.
- `EdgePropertyViewNotUsedAsTarget`: a view with edge properties is the target
  of no relation.
- `EdgePropertyViewSharedAcrossEdges`: a view with edge properties is the target
  of relations that follow more than one edge type.

When the artifacts are registered, an invalid view does not stop any graph from
registering. Everything else is registered first, and then the first invalid
view is thrown, with the others attached as suppressed exceptions. Graphs
without views behave exactly as before.

In the Kotlin and Java client a view is declared, checked and turned into a
definition. Executing a read through a view needs a host stack that is not
provided for Kotlin yet.

## Not in the Kotlin and Java client yet

- An in-process test scenario for graphs. The kernel's in-memory graph sink
  runs a graph without a database.
- Reading a graph through its views. Views are declared and checked, but the
  read is executed by a host stack that is not provided for Kotlin.
- Naming a sink configuration for a graph, and mapping one operation per item
  of an event's collection (`forEach`).
