# Event Store Subscriptions

This page shows how to subscribe one event store to another's outbox using
the Chronicle Kotlin client. A subscription pulls events from a source
event store's outbox into this event store's inbox, where reactors and
projections can observe them like any other event. See
[Subscriptions](/chronicle/subscriptions/) for the concept this page
assumes.

## Subscribing with a filter

`subscribe` takes a stable identifier for the subscription, the name of the
source event store, and a callback for configuring which event types to
pull in:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventType

@EventType(id = "payroll-run-completed")
data class PayrollRunCompleted(val employeeId: String, val amount: Double)
```

<!-- validate: body needs=store -->

```kotlin
store.eventStoreSubscriptions.subscribe(
    "payroll-inbox",
    "PayrollEventStore"
) { builder ->
    builder.withEventType(PayrollRunCompleted::class)
}
```

Call `withEventType` once per event type you want to receive.

## Subscribing to everything

Leave the builder unconfigured to subscribe to every event type in the
source outbox:

<!-- validate: body needs=store -->

```kotlin
store.eventStoreSubscriptions.subscribe(
    "payroll-firehose",
    "PayrollEventStore"
) {
    // No withEventType calls — every event type is subscribed to.
}
```

## Naming subscriptions

The subscription id should be stable and descriptive — it's how you target
the subscription later with `unsubscribe`, and it survives service restarts:

<!-- validate: body needs=store -->

```kotlin
store.eventStoreSubscriptions.subscribe(
    "payroll-inbox-v1",
    "PayrollEventStore"
) { builder ->
    builder.withEventType(PayrollRunCompleted::class)
}
```

## Unsubscribing

<!-- validate: body needs=store -->

```kotlin
store.eventStoreSubscriptions.unsubscribe("payroll-inbox")
```

## Listing subscriptions

<!-- validate: body needs=store -->

```kotlin
val subscriptions = store.eventStoreSubscriptions.getAll()
subscriptions.forEach { subscription ->
    println("${subscription.identifier} <- ${subscription.sourceEventStore}")
}
```

## Handling inbound events with a reactor

Once events start arriving through the subscription's inbox, a normal
`@Reactor` handles them exactly like locally-appended events:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reactor

@Reactor
class PayrollInboxReactor {
    fun payrollRunCompleted(event: PayrollRunCompleted, context: EventContext) {
        // Handles PayrollRunCompleted events pulled in from the outbox.
    }
}
```

You can also project the incoming events into a read model — see the
[projections guide](projections.md) for `@NotRewindable`, which is worth
considering for projections built from subscribed events, since a rewind
depends on the source system being able to redeliver them.

## Best practices

- Give every subscription a stable id up front — renaming it later is
  effectively unsubscribing and resubscribing from scratch.
- Filter with `withEventType` unless you genuinely need every event type
  from the source outbox; a narrow filter keeps the inbox smaller and
  reactor/projection registration explicit about what it depends on.
- Mark projections fed by a subscription `@NotRewindable` unless you've
  verified the source system's outbox can redeliver events far enough back.
