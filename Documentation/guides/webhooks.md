# Webhooks

This page shows how to register webhooks using the Chronicle Kotlin client.
A webhook is an observer that posts matching events to an external HTTP
endpoint as they're appended, instead of driving a reducer or projection.
See [Webhooks](/chronicle/webhooks/) for the concept this page assumes.

## Discoverable webhooks

Annotate a class with `@Webhook`, implement `IWebhookDefiner`, and configure
the target event types and authentication with the builder passed to
`define`:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.webhooks.IWebhookDefiner
import io.cratis.chronicle.webhooks.IWebhookDefinitionBuilder
import io.cratis.chronicle.webhooks.Webhook

@Webhook(targetUrl = "https://hooks.example.com/hr/employee-hired")
class EmployeeHiredWebhook : IWebhookDefiner {
    override fun define(builder: IWebhookDefinitionBuilder) {
        builder
            .withEventType(EmployeeHired::class)
            .withBearerToken("webhook-token")
    }
}
```

Pass instances to `register` — Chronicle discovers the `@Webhook` annotation
and calls `define` for you:

<!-- validate: body needs=store -->

```kotlin
store.webhooks.register(EmployeeHiredWebhook())
```

If `withEventType` is never called, the webhook observes every event type on
the event store.

## Imperative registration

You can also register a webhook without a dedicated class, supplying the id
and target URL directly:

<!-- validate: body needs=store -->

```kotlin
store.webhooks.register(
    "order-placed-webhook",
    "https://hooks.example.com/orders"
) { builder ->
    builder
        .withEventType(OrderPlaced::class)
        .withBearerToken("webhook-token")
}
```

## Authentication

`IWebhookDefinitionBuilder` supports basic, bearer token, and OAuth
authentication, plus arbitrary headers:

<!-- validate: skip -->

```kotlin
builder.withBasicAuth("username", "password")
builder.withBearerToken("token")
builder.withOAuth("https://auth.example.com", "client-id", "client-secret")
builder.withHeader("X-Custom-Header", "value")
```

## Targeting a different event sequence

By default a webhook observes the event log. Use `onEventSequence` to
target a different sequence:

<!-- validate: skip -->

```kotlin
import io.cratis.chronicle.eventSequences.EventSequenceId

builder.onEventSequence(EventSequenceId("outbox"))
```

## Replay and activation

A webhook is replayable and active by default. Opt out of either with
`notReplayable()` and `notActive()`:

<!-- validate: skip -->

```kotlin
builder.notReplayable()
builder.notActive()
```

## Listing and removing webhooks

<!-- validate: body needs=store -->

```kotlin
val webhooks = store.webhooks.getAll()
webhooks.forEach { webhook ->
    println("${webhook.identifier} -> ${webhook.target.url}")
}

store.webhooks.remove("order-placed-webhook")
```

## Best practices

- Prefer the discoverable `@Webhook`/`IWebhookDefiner` style for webhooks
  that are part of your application's startup registration — it keeps the
  target URL and event type filter next to each other and out of `main`.
- Reach for imperative registration when the target URL or filter is only
  known at runtime (e.g. per-tenant integrations).
- Keep webhook payload consumers idempotent — a replay can redeliver events
  a receiver already processed.
- Store secrets (bearer tokens, OAuth client secrets) outside source control
  and load them into the builder at startup.
