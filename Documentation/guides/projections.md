---
sharedTopicBridge: true
---

# Projections

Projections are shared Chronicle read-model behavior. Use the shared docs
for projection styles, model-bound projections, declarative projections,
and client-tabbed examples.

- [Projections](/chronicle/projections/)
- [Choosing a read model
  style](/chronicle/projections/choosing-a-read-model-style/)
- [Model-bound projections](/chronicle/projections/model-bound/)
- [Declarative projections](/chronicle/projections/declarative/)
- [Kotlin and Java client setup](../get-started/)

## Kotlin model-bound attributes

Beyond `@FromEvent` and `@SetFrom`, the Kotlin client has annotations for
structural shapes (joins, children, nested objects), arithmetic
(counters, running totals), catch-all mappings, and rewind behavior. Full
parameter tables are in the [annotation reference](../reference/annotations.md).

| Attribute | Use it for |
| --- | --- |
| `@Join` | Pulling in a property from another event type by id. |
| `@ChildrenFrom` | A collection built from child instances of an event type. |
| `@Nested` | A nullable sub-object built from its own `@FromEvent`. |
| `@ClearWith` | Which event clears (nulls out) a `@Nested` property. |
| `@Count` | An occurrence counter for a specific event type. |
| `@Increment` / `@Decrement` | Bumping a numeric property by one. |
| `@AddFrom` / `@SubtractFrom` | Adding/subtracting an event value in. |
| `@FromAll` / `@FromEvery` | A property populated from every event type. |
| `@NotRewindable` | Marking a projection as forward-only. |
| `@RemovedWith` | Which event removes an instance or a child. |
| `@RemovedWithJoin` | Like `@RemovedWith`, but resolving the id via a join. |
| `@NoAutoMap` | Disabling AutoMap for a type or a single property. |

`@NotRewindable` is worth considering for projections fed by events that
can't reliably be redelivered — e.g. from an
[event store subscription](/chronicle/subscriptions/explicit-subscriptions/).

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.projections.Increment
import io.cratis.chronicle.projections.Join
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class OrderOverview(
    val id: String = "",
    @Increment(OrderShipped::class) val shipmentCount: Int = 0,
    @Join(
        eventType = CustomerRegistered::class,
        on = "customerId",
        eventPropertyName = "email"
    )
    val customerEmail: String = ""
)
```

## Richer declarative projections

`IProjectionBuilderFor<T>` also supports `.join()`, `.fromEvery()`/`.fromAll()`,
`.removedWith()`/`.removedWithJoin()`, `.children()`, `.nested()`, and
`.notRewindable()` — the fluent equivalents of the attributes above, for
projections defined with a separate `IProjectionFor<T>` class instead of
model-bound annotations:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

class OrderOverviewProjection : IProjectionFor<OrderOverview> {
    override fun define(builder: IProjectionBuilderFor<OrderOverview>) {
        builder
            .from(OrderPlaced::class)
            .join(CustomerRegistered::class) { join ->
                join.on(OrderOverview::customerEmail)
                    .set(OrderOverview::customerEmail)
                    .toProperty("email")
            }
            .notRewindable()
    }
}
```
