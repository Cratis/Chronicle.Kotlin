---
sharedTopicBridge: true
---

# Transactions

Unit-of-work transactions are now documented as a shared Chronicle workflow
with synchronized Kotlin and Java examples.

- [Transactions and unit of work](/chronicle/events/transactions/)
- [Appending many events](/chronicle/events/appending-many/)
- [Kotlin and Java client setup](../get-started/)

## Kotlin client: `IUnitOfWork`

Beyond `commit`/`rollback`, `IUnitOfWork` (from `store.unitOfWorkManager`)
exposes the outcome of a commit:

| Member | Use it for |
| --- | --- |
| `isSuccess` | Whether every staged event committed cleanly. |
| `getConstraintViolations` | Constraint violations detected on commit. |
| `getConcurrencyViolations` | Concurrency violations detected on commit. |
| `getAppendErrors` | Append errors detected on commit. |
| `tryGetLastCommittedEventSequenceNumber` | Highest sequence committed. |
| `onCompleted` | A callback invoked once commit or rollback completes. |

See the
[EventStore API reference](../reference/event-store-api.md#unit-of-work)
for the full `IUnitOfWork`/`IUnitOfWorkManager` interfaces.

<!-- validate: body needs=store -->

```kotlin
val unitOfWork = store.unitOfWorkManager.begin()
unitOfWork.onCompleted { completed ->
    println("Unit of work ${completed.correlationId} completed")
}
```
