---
title: Projections
description: Find the shared guide to projections that build read models from events, with Kotlin and Java tabs showing how to declare them in the JVM client.
sharedTopicBridge: true
---

Projections are shared Chronicle read-model behavior. Use the shared docs
for projection styles, model-bound projections, declarative projections,
and client-tabbed examples.

- [Projections](/chronicle/projections/)
- [Choosing a read model
  style](/chronicle/projections/choosing-a-read-model-style/)
- [Model-bound projections](/chronicle/projections/model-bound/)
- [Declarative projections](/chronicle/projections/declarative/)
- [Kotlin and Java client setup](../get-started/)
- [Graph projections](graph-projections.md): project events onto a property
  graph of nodes and edges instead of a document read model.

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

## Dictionary entries, set once and the author

A `Map<String, V>` property on a read model holds one entry per key. The
key comes from each event, so one projection keeps a counter per category, a
total per currency or a value per author. Pass a `DictionaryKey` to
`count`, `increment`, `decrement`, `add`, `subtract`, `set` or `setOnce`:

- `DictionaryKey.fromEvent("category")` reads the key from the event content.
  Chronicle checks that the property exists on the event when the projection
  registers. The check is case-sensitive.
- `DictionaryKey.fromContext("eventType.id")` reads the key from the event
  context.
- `DictionaryKey.fromCausedBy(CausedByProperty.SUBJECT)` keys the entry by
  whoever caused the event.

`removeKey` removes the entry for the event's key. A key that isn't in the
dictionary leaves it unchanged.

`setOnce` sets a property only while it has no value. The first event that
produces a value wins, and replay gives the same result. `toCausedBy` sets a
property from the identity of whoever caused the event: its `SUBJECT`,
`NAME` or `USER_NAME`.

`DictionaryKey.fromEvent`, `removeKey` and `setOnce` need Chronicle 19.150.0
or later. An older kernel rejects the projection when it registers.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.CausedByProperty
import io.cratis.chronicle.projections.DictionaryKey
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class ExpenseRecorded(val category: String, val amount: Long)

@EventType
data class ExpenseCategoryClosed(val category: String)

@ReadModel
data class ExpenseReport(
    val id: String = "",
    val totalByCategory: Map<String, Long> = emptyMap(),
    val firstRecordedBy: String = "",
)

class ExpenseReportProjection : IProjectionFor<ExpenseReport> {
    override fun define(builder: IProjectionBuilderFor<ExpenseReport>) {
        builder
            .from(ExpenseRecorded::class) { from ->
                from.add(ExpenseReport::totalByCategory, DictionaryKey.fromEvent("category")).with("amount")
                from.setOnce(ExpenseReport::firstRecordedBy).toCausedBy(CausedByProperty.NAME)
            }
            .from(ExpenseCategoryClosed::class) { from ->
                from.removeKey(ExpenseReport::totalByCategory, DictionaryKey.fromEvent("category"))
            }
    }
}
```

On a model-bound read model, `@SetFrom` and `@SetFromContext` take
`setOnce = true`.

## Ordering children

By default, a new child goes to the end of its collection. Call `prepend()` to
put the newest child first, or `orderBy`/`orderByDescending` to keep the
collection sorted by a property on the child. A children builder takes one
ordering; a second call throws `ChildrenOrderingAlreadyDefined`. On a
model-bound read model, `@ChildrenFrom` takes `prepend`, `orderBy` or
`orderByDescending`.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class ReviewSubmitted(val reviewId: String, val rating: Int)

data class Review(val reviewId: String = "", val rating: Int = 0)

@ReadModel
data class ProductReviews(val id: String = "", val reviews: List<Review> = emptyList())

class ProductReviewsProjection : IProjectionFor<ProductReviews> {
    override fun define(builder: IProjectionBuilderFor<ProductReviews>) {
        builder.children(ProductReviews::reviews, Review::class) { children ->
            children
                .identifiedBy("reviewId")
                .orderByDescending(Review::rating)
                .from(ReviewSubmitted::class) { it.usingKey("reviewId") }
        }
    }
}
```

Children ordering needs Chronicle 19.150.0 or later. An older kernel ignores
the ordering and keeps children in arrival order.

## When to use a projection instead of a reducer

Use a projection when each event maps onto the read model by declaration:
copying values, counting, keeping totals per key, keeping the first value
seen, or keeping an ordered list of children. Chronicle runs the mapping, so
it rebuilds the same way on replay and works with every sink.

Use a [reducer](reducers.md) when the next state depends on decisions that
are easier to write as code, such as branching on the current state,
calculations across several properties, or rules that guard against
out-of-order events.
