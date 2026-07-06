# Chronicle Kotlin Client

The Chronicle Kotlin client is the JVM SDK for Chronicle. It wraps the
Chronicle gRPC contracts with Kotlin-native constructs such as coroutines,
data classes, and annotations.

Use this section for Kotlin and Java setup, runtime integration,
annotations, and API reference details. Shared Chronicle concepts and
workflows live in the main Chronicle docs and use language tabs when code
differs by client.

## Shared Chronicle topics

- [Get started](/chronicle/get-started/)
- [Events and event logs](/chronicle/events/)
- [Appending events](/chronicle/events/appending/)
- [Read models](/chronicle/read-models/)
- [Projections](/chronicle/projections/)
- [Reactors](/chronicle/reactors/)
- [Reducers](/chronicle/reducers/)
- [Constraints](/chronicle/constraints/)
- [Event seeding](/chronicle/event-seeding/)
- [Compliance](/chronicle/compliance/)
- [Transactions and unit of work](/chronicle/events/transactions/)
- [Event evolution](/chronicle/understanding-event-evolution/)

## Kotlin and Java setup

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

The same JVM client supports both Kotlin and Java. Shared pages include
separate Kotlin and Java tabs when both examples exist.

## Client-specific pages

- [Get Started](get-started/) — install the JVM client, connect, and run a
  small Kotlin flow
- [Reference](reference/) — annotations, configuration, and service API details
