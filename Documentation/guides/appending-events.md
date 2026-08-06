---
sharedTopicBridge: true
---

# Appending Events

Appending events is documented in the shared Chronicle docs with
synchronized examples for C#, Kotlin, Java, Elixir, and TypeScript.

- [Appending events](/chronicle/events/appending/)
- [Appending many events](/chronicle/events/appending-many/)
- [Event source identity](/chronicle/events/event-source-id/)
- [Concurrency](/chronicle/events/concurrency/)

Use the [Kotlin get started
page](/chronicle/clients/kotlin/get-started/) for JVM setup before running
the shared examples.

## Kotlin client: `IEventSequence` and `IEventLog`

Beyond `append`/`appendMany`, the Kotlin client's `store.eventLog` (and any
sequence from `store.getEventSequence`) exposes a richer surface for
reading, redacting, and observing events:

| Member | Use it for |
| --- | --- |
| `appendMany(events)` | One atomic batch spanning several event sources. |
| `getTailSequenceNumber` | Current end of the sequence or one source. |
| `getForEventSourceIdAndEventTypes` | Events for one source, by type. |
| `getFromSequenceNumber` | Events forward from a position (a bookmark). |
| `getNextSequenceNumber` | Sequence number the next append will get. |
| `completeStream` | Closes a stream so it can't be appended to again. |
| `redact`/`redactForEventSource` | Erases event content — see below. |
| `appendOperations` | Hot `Flow` of appends made through this instance. |

See the [EventStore API reference](../reference/event-store-api.md) for the
full `IEventSequence` interface and `ConcurrencyScope` (optimistic
concurrency via `AppendOptions.concurrencyScope`).

### Appending across event sources

`appendMany(eventSourceId, events)` shapes the whole batch around one event
source. When a single unit of work touches several — moving money between
two accounts, say — pass `EventToAppend` records instead. Each carries its
own event source id and its own shaping, and the batch still commits as
one atomic append:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class OrderLineAdded(val sku: String, val quantity: Int)

@EventType
data class StockReserved(val sku: String, val quantity: Int)
```

<!-- validate: body needs=store -->

```kotlin
import io.cratis.chronicle.eventSequences.EventToAppend

store.eventLog.appendMany(
    listOf(
        EventToAppend("order-1", OrderLineAdded("sku-9", 2)),
        EventToAppend("sku-9", StockReserved("sku-9", 2))
    )
)
```

Pass `concurrencyScopes` to check specific event sources optimistically —
it is keyed by event source id, and any source left out is appended
unchecked.

### Composing a batch across call sites

Sometimes the events for one unit of work are not all decided in the same
place. `forEventSourceId` starts a composed operation you can build up and
inspect before anything is sent, then commit with a single `perform()`:

<!-- validate: body needs=store -->

```kotlin
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.operations.forEventSourceId

val operations = store.eventLog.forEventSourceId("order-1") {
    withConcurrencyScope {
        withSequenceNumber(EventSequenceNumber(4)).withEventSourceId()
    }
    append(OrderLineAdded("sku-9", 2))
}

operations.forEventSourceId("sku-9") {
    append(StockReserved("sku-9", 2), tags = listOf("inventory"))
}

// Nothing has reached the kernel yet - this is exactly what perform() will send.
println(operations.getEventsToAppend())

val results = operations.perform()
```

Concurrency lives on the event source rather than on an individual event,
because that is where the kernel checks it. A source that never asks for a
scope is appended unchecked, and a scope already set is never cleared by a
later call that expresses no expectation.

### Redacting events

`redact` and `redactForEventSource` permanently rewrite event content — a
destructive, irreversible operation, not a soft delete or a field mask.
Once either call returns, the original content is gone from the event
store for good. Use them only for a confirmed compliance/erasure request:

<!-- validate: body needs=store -->

```kotlin
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.RedactionReason

val reason = RedactionReason("GDPR erasure request")
store.eventLog.redact(EventSequenceNumber(42), reason)
store.eventLog.redactForEventSource("customer-1", reason)
```
