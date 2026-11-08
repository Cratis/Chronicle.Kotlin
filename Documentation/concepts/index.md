---
title: Concepts
description: Find the shared Chronicle concept pages for events, observers and read models, and see which JVM pages show the matching Kotlin and Java client syntax.
sharedTopicBridge: true
---

Events record what happened. Observers process those events; projections
and reducers build read models that applications can query.

Start with the shared Chronicle concepts in this order:

1. [Events](/chronicle/concepts/event/) — the facts you record.
2. [Event sources](/chronicle/concepts/event-source/) — the identity those
   facts belong to.
3. [Observers](/chronicle/concepts/observers/) — how events are consumed.
4. [Read models](/chronicle/read-models/) — views built from those events.

Then choose by task. To build a read model, use
[Projections](/chronicle/projections/) for declarative mappings or
[Reducers](/chronicle/reducers/) for state-transition code. To run side
effects in response to events, use [Reactors](/chronicle/reactors/).

The shared pages teach these concepts; the Kotlin client pages cover
Kotlin and Java setup, annotations, and API details.

For JVM-specific syntax, see the [annotation
reference](/chronicle/clients/kotlin/reference/annotations/).
