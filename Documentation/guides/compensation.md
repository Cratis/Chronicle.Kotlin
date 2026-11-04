---
title: Compensating events
description: Declare compensating event types with @CompensationFor and append them with compensate in the Chronicle Kotlin client.
---

A compensating event records, as a new fact, that an earlier event is being
offset, such as a reversal entry in a ledger. Chronicle links the two by
sequence number and never changes the original. For the concept and the
rules the kernel applies, see [Event
compensation](/chronicle/events/compensation/).

## Declare a compensation event type

Annotate the compensating event type with `@CompensationFor`, passing the
event type it compensates for. Both classes need `@EventType`:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.CompensationFor
import io.cratis.chronicle.events.EventType

@EventType("entry-posted")
data class EntryPosted(val account: String, val amount: Long)

@EventType("entry-reversed")
@CompensationFor(EntryPosted::class)
data class EntryReversed(val reason: String)
```

Chronicle stores the link on the event type's schema, as its
`compensationFor` entry, so every client and the Workbench can discover
which compensations exist for an event type. A `@CompensationFor` whose
target is not an `@EventType` fails when the schema is built.

## Compensate an event

Name the event you are compensating by its sequence number, instead of an
event source. The compensation goes to the event source, event stream and
subject of that event, so you do not pass them:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.AppendError
import io.cratis.chronicle.eventSequences.EventSequenceNumber

suspend fun reverseEntry(store: IEventStore, postedAt: Long) {
    val result = store.eventLog.compensate(
        EventSequenceNumber(postedAt),
        EntryReversed("Posted to the wrong account")
    )

    if (!result.isSuccess) {
        result.errors.forEach { error ->
            when (error.message) {
                AppendError.COMPENSATED_EVENT_NOT_FOUND ->
                    println("There is no event at $postedAt")
                AppendError.EVENT_TYPE_DOES_NOT_COMPENSATE_EVENT_TYPE ->
                    println("EntryReversed does not compensate that event")
            }
        }
    }
}
```

A rejected compensation is reported in the result's `errors`; it is not
thrown. Constraints, concurrency checks and compliance run for the
compensating event like any other append, and the same event can be
compensated more than once if your domain allows it.

Pass `CompensateOptions` for a `correlationId`, `tags`, `occurred` or a
`causation` chain. From Java, `BlockingEventSequence.compensate` does the
same without coroutines.

## Read the link

The event context carries the compensated sequence number, for reactors,
reducers and anything that reads events back:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventContext

fun compensatedBy(context: EventContext): Long? = context.compensationFor
```

`compensationFor` is `null` for an event that is not a compensation, and
`isCompensation` is the boolean form. The wire format cannot say "absent", so
a value of `0` reads as "not a compensation": a compensation of the event at
sequence number `0` is indistinguishable from none. Check the event type when
that matters.

## Testing

`InMemoryEventSequence` from the testing module supports `compensate` on the
kernel's terms: it rejects a missing original and an event type that is not
declared for the original's type, and appends to the original's event source.
