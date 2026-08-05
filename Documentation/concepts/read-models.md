---
sharedTopicBridge: true
---

# Read Models

Read models are documented in the shared Chronicle docs so the model stays
consistent across clients.

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

Beyond `getInstanceByKey`, the Kotlin client's `store.readModels` exposes a
richer surface for reading and managing read model instances:

| Member | Use it for |
| --- | --- |
| `getInstances` | Every instance, replaying events in-process. |
| `getSnapshotsById` | Snapshots of a read model grouped by correlation id. |
| `watch` | A `Flow` of changesets for a read model — a live view. |
| `dehydrateSession` | Releasing session-scoped state for an instance. |
| `release` | Decrypting `@Pii`-annotated properties on an instance. |
| `materialized` | Paginated, server-materialized reads — see below. |

```kotlin
val employees = store.readModels.getInstances(EmployeeDetails::class)
employees.forEach { employee ->
    println("${employee.firstName} ${employee.lastName}")
}
```

### Materialized (server-side) reads

`store.readModels.materialized` provides paginated access to instances a
sink has already materialized server-side, instead of replaying events
in-process:

```kotlin
val page = store.readModels.materialized.getInstances(
    EmployeeDetails::class,
    skip = 0,
    take = 50
)

store.readModels.materialized
    .observeInstances(EmployeeDetails::class, skip = 0, take = 50)
    .collect { page -> /* re-render whenever the page changes */ }
```

`getInstances` returns a single page; `observeInstances` returns a `Flow`
that emits a fresh page every time the underlying data changes.
