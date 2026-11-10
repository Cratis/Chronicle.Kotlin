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
`@EdgeRemovedWith`. Annotated nodes and edges, and `IGraphProjection`
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

## Not in the Kotlin and Java client yet

- An in-process test scenario for graphs. The kernel's in-memory graph sink
  runs a graph without a database.
- Naming a sink configuration for a graph, and mapping one operation per item
  of an event's collection (`forEach`).
