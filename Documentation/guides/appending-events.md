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
