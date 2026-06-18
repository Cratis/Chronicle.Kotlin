# Events

An event is an immutable fact. It records something that happened in your
domain — a business decision, a user action, or a system state change.
Events are never modified or deleted (except through explicit compliance
workflows).

## Defining an event type

Annotate a Kotlin data class with `@EventType`:

```kotlin
@EventType
data class OrderPlaced(
    val orderId: String,
    val customerId: String,
    val totalAmount: Double
)
```

By default, Chronicle derives the event type identifier from the **class
simple name** — `OrderPlaced` becomes the identifier automatically. You never
need to specify an `id` unless you want to override the default or use a
stable identifier that survives class renames:

```kotlin
@EventType(id = "order-placed")
data class OrderPlaced(...)
```

Use an explicit `id` when you need the identifier to remain stable
regardless of future refactoring.

### Generation

The `generation` parameter (default `1`) tracks the schema version of an
event. When you add fields or change the meaning of an event, increment the
generation and register a migration:

```kotlin
@EventType(generation = 2)
data class OrderPlaced(
    val orderId: String,
    val customerId: String,
    val totalAmount: Double,
    val currency: String   // added in generation 2
)
```

### Tombstone events

A tombstone event signals that an event source has been retired. Set
`tombstone = true`:

```kotlin
@EventType(tombstone = true)
data class OrderCancelled(val orderId: String, val reason: String)
```

## Event source identity

Every event belongs to an **event source** — the entity it describes. The
event source is identified by a plain string key you supply when appending:

```kotlin
store.eventLog.append(
    eventSourceId = "order-42",
    event = OrderPlaced(
        orderId = "order-42",
        customerId = "cust-7",
        totalAmount = 149.99
    )
)
```

All events with the same `eventSourceId` form the complete history of that entity.

## Event context

When an observer receives an event, it can also receive an `EventContext` with metadata:

```kotlin
@Reactor
class AuditReactor {
    fun onOrderPlaced(event: OrderPlaced, context: EventContext) {
        println("Occurred at ${context.occurred}, sequence ${context.sequenceNumber}")
    }
}
```

| Field | Type | Description |
| --- | --- | --- |
| `sequenceNumber` | `Long` | Position in the event log |
| `eventSourceId` | `String` | The entity this event belongs to |
| `eventType` | `EventTypeDescriptor` | The type identifier and generation |
| `occurred` | `Instant` | When the event was appended |
| `correlationId` | `UUID` | Traces the causal chain across services |
| `causedBy` | `Identity` | The identity that triggered the event |
