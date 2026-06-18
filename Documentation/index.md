# Chronicle Kotlin Client

The Chronicle Kotlin client is an idiomatic Kotlin library for building
event-sourced applications with
[Cratis Chronicle](https://github.com/Cratis/Chronicle). It wraps the
Chronicle gRPC contracts with Kotlin-native constructs — coroutines, data
classes, and annotations.

## What you can build

- **Event-driven domains** — append typed, immutable events to an event log
  and have the system react to them
- **Real-time read models** — fold events into queryable state using reducers
  or projections
- **Behavioral automation** — react to events across your system with reactors
- **Integrity guarantees** — enforce uniqueness constraints at the event store
  level
- **PII compliance** — annotate sensitive fields so Chronicle can manage their
  lifecycle

## Before you start

You need a running Chronicle Kernel. The simplest way is Docker Compose:

```yaml
services:
  chronicle:
    image: cratis/chronicle:latest
    ports:
      - "35000:35000"
      - "35001:35001"
```

Then add the client to your Gradle build:

```kotlin
dependencies {
    implementation("io.cratis:chronicle:0.1.0")
}
```

## Where to go next

- [**Get Started**](get-started/toc.yml) — build your first event-sourced
  feature in minutes
- [**Guides**](guides/toc.yml) — recipes for specific tasks (reactors,
  projections, constraints, seeding)
- [**Concepts**](concepts/toc.yml) — understand how the pieces fit together
- [**Reference**](reference/toc.yml) — complete API and annotation reference
