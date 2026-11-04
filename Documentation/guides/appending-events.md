---
title: Appending Events
description: Where the shared guide to appending events lives, with Kotlin and Java tabs.
sharedTopicBridge: true
---

In Kotlin, `store.eventLog.append(...)` is a suspending call. Java uses
`store.getEventLog().append(...)` on a `BlockingEventStore`. Both return an
`AppendResult`: keep it and check `isSuccess` (Java: `isSuccess()`) before using
the accepted sequence number. Java reads that position with
`getSequenceNumberValue()`, not the Kotlin value-class getter.

For the declaration, registration and result-handling steps, follow
[Artifact registration](artifact-registration.md#the-short-version), using the
existing event declaration and configured client from
[Get started](/chronicle/clients/kotlin/get-started/). A completed registration
wait is not an acceptance check. Inspect returned constraint violations,
errors and concurrency violations; a command or transport failure can throw
instead. Do not automatically append again after an unsuccessful result or a
lost response: an unconfirmed outcome does not establish that nothing was
stored. Once accepted, an event can still be ahead of its
[materialized read model](../reference/connection-lifecycle.md#reading-after-writing).

Ordinary appends need no named source definition. Use
[Event sources and streams](event-sources.md) to add one when your application
needs that routing. The shared Chronicle guides explain the append contract
with language-specific examples:

- [Appending events](/chronicle/events/appending/)
- [Appending many events](/chronicle/events/appending-many/)
- [Event source identity](/chronicle/events/event-source-id/)
- [Concurrency](/chronicle/events/concurrency/)

For the breaking change to omitted routing defaults and explicit legacy
routes, see [Migrate append routing](migrate-append-routing.md).

## Date and time properties

Event and read model properties can use the following `java.time` types.
These date and time values are written as ISO-8601 strings matching their
registered schema formats:

| Type | Schema `format` | Written as |
| --- | --- | --- |
| `Instant` | `date-time-offset` | `2024-05-01T10:15:30Z` |
| `OffsetDateTime` | `date-time-offset` | `2024-05-01T10:15:30+02:00` |
| `ZonedDateTime` | `date-time-offset` | `2024-05-01T10:15:30+02:00` |
| `LocalDateTime` | `date-time` | `2024-05-01T10:15:30` |
| `LocalDate` | `date` | `2024-05-01` |
| `LocalTime` | `time` | `10:15:30` |

A `ZonedDateTime` is written as an offset date-time, so it keeps the instant
and the offset but not the zone id. Read back, its zone is the offset
(`+02:00`), not `Europe/Oslo`. Use `Instant` or `OffsetDateTime` when the zone
does not matter, or store the zone id in a separate string property when it
does.

`java.time.Duration` has its own adapter: it is written as a TimeSpan-style
string with 100-nanosecond precision, truncating sub-tick nanoseconds toward
zero. Do not rely on a round trip to retain finer precision. Its reader accepts
supported TimeSpan and ISO-8601 duration strings. `ByteArray` (Java: `byte[]`)
is written as standard padded base64. Their registered schema formats are
`duration` and `byte-array`, respectively; they are not additional ISO-8601
date/time rows.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventType
import java.time.Instant
import java.time.LocalDate

@EventType
data class ShipmentDispatched(val dispatchedAt: Instant, val deliveryDate: LocalDate)
```

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
two accounts, say — pass `EventForEventSourceId` records instead. Each carries its
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
import io.cratis.chronicle.eventSequences.EventForEventSourceId

store.eventLog.appendMany(
    listOf(
        EventForEventSourceId("order-1", OrderLineAdded("sku-9", 2)),
        EventForEventSourceId("sku-9", StockReserved("sku-9", 2))
    )
)
```

Keep the list of `AppendResult` values returned by `appendMany` and inspect
their success and failure fields before continuing. The atomic batch is not a
reason to ignore its response or replay it automatically after a lost response.

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

Inspect the `results` returned by `perform()` just as you would those from
`appendMany`; building or printing an operation has not confirmed acceptance.

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
