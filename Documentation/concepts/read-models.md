---
title: Read Models
description: Where the shared Chronicle explanation of read models lives, for Kotlin and Java readers.
sharedTopicBridge: true
---

For the concepts behind a read model and how to build one, use the shared
Chronicle pages:

- [Read models](/chronicle/read-models/)
- [Getting a single read model
  instance](/chronicle/read-models/getting-single-instance/)
- [Projections](/chronicle/projections/)
- [Reducers](/chronicle/reducers/)

For JVM syntax, see the [annotation
reference](/chronicle/clients/kotlin/reference/annotations/) and
[EventStore API
reference](/chronicle/clients/kotlin/reference/event-store-api/).

## Kotlin client: `IReadModelsService`

The Kotlin excerpts below use `EmployeeProfile` from the
[Kotlin read model setup](../get-started/index.md#kotlin-read-model) and an
existing `store` from that walkthrough. Run them inside a coroutine;
class declarations, connection and registration setup are omitted here.

For a one-time read of every instance, call `store.readModels.getInstances`.
The result is a list of profiles; the loop prints each employee's first
and last name. Use `getInstanceByKey` when you need one instance instead.

<!-- validate: body needs=store -->

```kotlin
val employees = store.readModels.getInstances(EmployeeProfile::class)
employees.forEach { employee ->
    println("${employee.firstName} ${employee.lastName}")
}
```

For history, live changes or paginated reads, choose the operation that
matches the task:

| Member | Use it for |
| --- | --- |
| `getSnapshotsById` | Historical snapshots for a read model key, grouped by correlation id. |
| `watch` | A `Flow` of live changesets for a read model. |
| `materialized` | Paginated, server-materialized reads — see below. |

`getSnapshotsById` and `watch` both deserialize straight into the read
model type you ask for — `getSnapshotsById(EmployeeProfile::class, key)`
returns `List<ReadModelSnapshot<EmployeeProfile>>`, and
`watch(EmployeeProfile::class)` returns
`Flow<ReadModelChangeset<EmployeeProfile>>` — no manual JSON parsing
either way.

To react to changes without collecting a `Flow` yourself, write a
convention-dispatched reactor instead — see [Read Model
Reactors](../guides/read-model-reactors.md).

Session cleanup and privacy release are separate operations.
`dehydrateSession` releases session-scoped state for an instance;
`release`/`releaseMany` decrypt `@Pii` properties on one or many instances.

### Materialized (server-side) reads

`store.readModels.materialized` provides paginated access to instances a
sink has already materialized server-side:

<!-- validate: body needs=store -->

```kotlin
val page = store.readModels.materialized.getInstances(
    EmployeeProfile::class,
    skip = 0,
    take = 50
)

store.readModels.materialized
    .observeInstances(EmployeeProfile::class, skip = 0, take = 50)
    .collect { updatedPage -> /* re-render whenever the page changes */ }
```

`getInstances` returns a single page; `observeInstances` returns a `Flow`
that emits a fresh page every time the underlying data changes.
