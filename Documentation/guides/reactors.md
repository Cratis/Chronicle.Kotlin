# Reactors

A reactor observes events and performs side effects. It does not build persistent state.

## Define a reactor

```kotlin
@Reactor
class OrderNotifications(private val emailService: EmailService) {
    fun onOrderPlaced(event: OrderPlaced) {
        emailService.send("Order ${event.orderId} placed for ${event.customerId}")
    }

    fun onOrderCancelled(event: OrderCancelled, context: EventContext) {
        emailService.send(
            "Order ${event.orderId} cancelled at ${context.occurred}: ${event.reason}"
        )
    }
}
```

**Rules:**
- Each public method handles exactly one event type, determined by the type of its first parameter
- The second parameter `EventContext` is optional — include it only when you need metadata
- The class must be annotated with `@Reactor`

## Register the reactor

```kotlin
val job = store.reactors.register(OrderNotifications(emailService))
```

`register` returns a `Job`. The reactor keeps running until the job is cancelled:

```kotlin
job.cancel() // stop observing
```

## Override the reactor ID

By default the reactor ID is the **class simple name** — `OrderNotifications` becomes the identifier automatically. Override it only when you need the identifier to remain stable regardless of future class renames:

```kotlin
@Reactor(id = "order-notifications")
class OrderNotifications { ... }
```

## Design for idempotency

Chronicle may replay your reactor from the beginning of the event log after a restart. Guard against duplicate side effects using idempotency keys, database upserts, or check-then-act patterns at the target service.
