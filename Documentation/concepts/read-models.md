# Read Models

A read model is a queryable view of state, derived from events. Chronicle
maintains read models automatically — you define the shape and the mapping;
Chronicle keeps them up to date.

## Two ways to build read models

| Approach | Best for | Annotation |
| --- | --- | --- |
| **Reducer** | Fold all events for one source | `@Reducer` |
| **Projection** | Map specific event fields | `@Projection` |

## Read model identity

Annotate the read model class with `@ReadModel`. Chronicle derives the
identifier from the **class simple name** by default — `OrderSummary` becomes
the identifier automatically. Supply an explicit `id` only when you need the
identifier to remain stable regardless of future class renames:

```kotlin
@ReadModel
data class OrderSummary(
    val orderId: String = "",
    val status: String = "pending"
)
```

To override the default:

```kotlin
@ReadModel(id = "order-summary")
data class OrderSummary(...)
```

Every property must have a default value — Chronicle deserializes read models
from JSON and missing fields fall back to their defaults.

## Querying

Retrieve a read model instance by its event source key:

```kotlin
val summary: OrderSummary? = store.readModels.getInstanceByKey(
    readModelClass = OrderSummary::class,
    key = "order-42"
)
```

Returns `null` when no events have been projected for that key yet.

## Projections

A projection maps event fields to read model fields declaratively. Implement
`IProjectionFor<T>` and call `from()` for each event type you want to map:

```kotlin
@Projection
class OrderSummaryProjection : IProjectionFor<OrderSummary> {
    override fun define(builder: IProjectionBuilderFor<OrderSummary>) {
        builder
            .from(OrderPlaced::class) { it
                .set(OrderSummary::orderId).toProperty("orderId")
                .set(OrderSummary::totalAmount).toProperty("totalAmount")
            }
            .from(OrderCancelled::class) { it
                .set(OrderSummary::status).toProperty("reason")
            }
    }
}
```

Alternatively, use the `@FromEvent` and `@SetFrom` annotations directly on the
read model fields for a fully declarative style.

## Registering read models

Read models must be registered before they receive events:

```kotlin
store.readModels.register(OrderSummary::class)
store.projections.register(OrderSummaryProjection())
```
