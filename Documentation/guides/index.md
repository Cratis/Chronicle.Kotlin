---
title: Guides
description: Where to find Chronicle task guides for Kotlin and Java, and the guides that only apply to the JVM client.
sharedTopicBridge: true
---

Chronicle task guides are shared across clients. They live in the main
Chronicle docs and use synchronized language tabs for Kotlin, Java, C#,
Elixir, and TypeScript examples.

Start with:

- [Appending events](/chronicle/events/appending/)
- [Read models](/chronicle/read-models/)
- [Projections](/chronicle/projections/)
- [Reactors](/chronicle/reactors/)
- [Reducers](/chronicle/reducers/)
- [Constraints](/chronicle/constraints/)
- [Event evolution](/chronicle/understanding-event-evolution/)

## Guides for the JVM client

These cover what only the Kotlin and Java client does:

- [Wiring Chronicle into Spring Boot](spring-boot.md)
- [Controlling what gets registered, and when](artifact-registration.md)
- [Testing a slice without a kernel](testing.md)
- [Declaring strongly-typed identifiers](concepts.md)
- [Seeding events](seeding.md)
- [Reacting to read model changes](read-model-reactors.md)
- [Capturing external sources](captures.md)
- [Registering external services](external-services.md)
- [Tracing with OpenTelemetry](tracing.md)
- [Migrating append routing](migrate-append-routing.md)

Use the Kotlin client section for
[setup](/chronicle/clients/kotlin/get-started/) and [API
reference](/chronicle/clients/kotlin/reference/).
