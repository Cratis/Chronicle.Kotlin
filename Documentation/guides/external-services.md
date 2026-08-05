# External Services

This page shows how to register external services using the Chronicle
Kotlin client. An external service is a named endpoint — HTTP or database —
that Chronicle-side integrations (such as webhooks or reactor-triggered
calls) can address by name instead of embedding connection details
throughout your code. See [External Services](/chronicle/external-services/)
for the concept this page assumes.

## Registering an HTTP endpoint

`register` takes a name — also used as the service's identifier — and a
callback for configuring the endpoint:

```kotlin
store.externalServices.register("payroll-provider") { builder ->
    builder
        .http("https://payroll.example.com/api")
        .withBearerToken("payroll-integration-token")
}
```

## Authentication

`IExternalServiceBuilder` supports the same three authentication schemes as
webhooks, plus arbitrary headers:

```kotlin
builder.withBasicAuth("username", "password")
builder.withBearerToken("token")
builder.withOAuth("https://auth.example.com", "client-id", "client-secret")
builder.withHeader("X-Custom-Header", "value")
```

## Registering a database endpoint

Use `msSql` or `postgreSql` instead of `http` to describe a database
endpoint. `port` defaults to the provider's standard port when left at `0`:

```kotlin
store.externalServices.register("payroll-database") { builder ->
    builder.postgreSql(
        host = "payroll-db.internal",
        database = "payroll",
        username = "chronicle",
        password = "secret"
    )
}
```

Add provider-specific connection options with `withOption`:

```kotlin
builder
    .msSql(
        host = "payroll-db.internal",
        database = "payroll",
        username = "chronicle",
        password = "secret"
    )
    .withOption("Encrypt", "true")
```

## Best practices

- Register external services once at startup, next to your event type and
  observer registrations, so the set of integrations Chronicle knows about
  is easy to find.
- Give services stable, descriptive names — they're the identifier other
  Chronicle features (like webhooks) use to reference the service.
- Keep credentials out of source control; load them from configuration or a
  secret store before passing them to the builder.
