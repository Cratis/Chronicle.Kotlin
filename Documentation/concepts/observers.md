# Observers

An observer watches a stream of events and reacts to them. The Chronicle
Kotlin client supports two observer patterns: **reactors** and **reducers**
(reducers are a subset of the broader projection family).

## Reactors

A reactor performs **side effects** in response to events. It does not build
persistent state — it acts. Typical uses include sending notifications,
triggering commands in other services, and writing audit records.

```kotlin
@Reactor
class OrderNotifications {
    fun onOrderPlaced(event: OrderPlaced) {
        emailService.send("New order ${event.orderId} for customer ${event.customerId}")
    }

    fun onOrderCancelled(event: OrderCancelled) {
        emailService.send("Order ${event.orderId} was cancelled: ${event.reason}")
    }
}
```

**Dispatch rules:**

- One method per event type (first parameter determines the event type)
- Method name is arbitrary
- The second parameter can be `EventContext` (optional, for metadata)

### Reactor identity

By default the reactor ID is the **class simple name** — `OrderNotifications`
becomes the identifier automatically. Override it with the `id` parameter only
when you need a stable identifier that survives class renames:

```kotlin
@Reactor(id = "order-notifications")
class OrderNotifications { ... }
```

## Reducers

A reducer **folds events into a read model**. Each handler receives the
current state and the event, and returns the next state. Chronicle stores the
result so your application can query it later.

```kotlin
@ReadModel
data class OrderSummary(
    val orderId: String = "",
    val status: String = "pending",
    val totalAmount: Double = 0.0
)

@Reducer
class OrderSummaryReducer {
    fun on(event: OrderPlaced, state: OrderSummary): OrderSummary =
        state.copy(orderId = event.orderId, totalAmount = event.totalAmount)

    fun on(event: OrderCancelled, state: OrderSummary): OrderSummary =
        state.copy(status = "cancelled")
}
```

Reducers run in-order, event by event, and are idempotent — Chronicle may
replay them from the beginning if the observer restarts.

## Replay and idempotency

Both reactors and reducers may be replayed from the beginning of the event
log. Design them accordingly:

- **Reactors** — guard against duplicate side effects (check-then-act or
  use idempotency keys)
- **Reducers** — pure fold functions are naturally idempotent; avoid
  external I/O inside a reducer

## Registering observers

Register observers on the `EventStore` before they start receiving events:

```kotlin
val store = client.getEventStore("MyApp")
store.reactors.register(OrderNotifications())
store.reducers.register(OrderSummaryReducer())
```

Registration returns a `Job` that keeps the observer alive. Cancel it to stop observing.
