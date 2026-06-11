# Reducers

A reducer folds a stream of events into a mutable read model object. Chronicle stores the latest state so your application can query it by event source key.

## Define the read model

```kotlin
@ReadModel
data class OrderSummary(
    val orderId: String = "",
    val customerId: String = "",
    val status: String = "pending",
    val totalAmount: Double = 0.0
)
```

Every property must have a default value — Chronicle deserializes the stored JSON and falls back to defaults for missing fields.

## Define the reducer

```kotlin
@Reducer
class OrderSummaryReducer {
    fun on(event: OrderPlaced, state: OrderSummary): OrderSummary =
        state.copy(
            orderId = event.orderId,
            customerId = event.customerId,
            totalAmount = event.totalAmount
        )

    fun on(event: OrderCancelled, state: OrderSummary): OrderSummary =
        state.copy(status = "cancelled")
}
```

**Rules:**
- One method per event type (determined by the first parameter type)
- The second parameter is the current state
- Return the next state — never mutate `state` in place
- The class must be annotated with `@Reducer`

## Register and query

```kotlin
store.reducers.register(OrderSummaryReducer())
store.readModels.register(OrderSummary::class)

// After events have been processed:
val summary: OrderSummary? = store.readModels.getInstanceByKey(OrderSummary::class, "order-42")
println(summary?.status) // "pending" or "cancelled"
```

## Handling initial state

On the first event for a new event source, `state` is a default-constructed instance of your read model class. Return a meaningful state from that first fold.
