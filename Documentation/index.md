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

You need a running Chronicle Kernel. The simplest way is Docker Compose.
Chronicle serves gRPC, the API, and health checks on port `35000` over TLS
with a self-signed development certificate:

```yaml
services:
  chronicle:
    image: cratis/chronicle:latest-development-slim
    depends_on:
      - mongodb
      - mongodb-init
    environment:
      - ASPNETCORE_ENVIRONMENT=Development
      - Cratis__Chronicle__Storage__Type=MongoDB
      - Cratis__Chronicle__Storage__ConnectionDetails=mongodb://mongodb:27017/?directConnection=true
    ports:
      - "35000:35000"

  mongodb:
    image: mongo:8.2
    command: ["mongod", "--replSet", "rs0", "--bind_ip_all"]
    ports:
      - "27017:27017"

  # Chronicle's MongoDB storage needs a replica set — initiate it once.
  mongodb-init:
    image: mongo:8.2
    depends_on:
      - mongodb
    restart: "no"
    command:
      - /bin/bash
      - -lc
      - |
        until mongosh --host mongodb --quiet \
          --eval "db.adminCommand('ping')" >/dev/null 2>&1; do
          sleep 1
        done
        mongosh --host mongodb --quiet --eval "
        try {
          rs.status();
        } catch (e) {
          rs.initiate({
            _id: 'rs0',
            members: [{ _id: 0, host: 'mongodb:27017' }]
          });
        }"
```

Chronicle is ready once `curl -sk https://localhost:35000/health` reports
`Healthy`. The `Samples/Kotlin/Console` and `Samples/Java/Console` folders
in the repository contain this file along with PostgreSQL, SQL Server, and
SQLite profiles.

Then add the client to your Gradle build:

<!-- validate: skip -->

```kotlin
dependencies {
    implementation("io.cratis:chronicle:2.1.2")
}
```

The same JVM client supports both Kotlin and Java. Shared pages include
separate Kotlin and Java tabs when both examples exist.

Spring Boot applications add the starter instead — it brings the client with it
and wires everything up:

<!-- validate: skip -->

```kotlin
dependencies {
    implementation("io.cratis:chronicle-spring-boot-starter:2.1.1")
}
```

## Client-specific pages

- [Get Started](get-started/) — install the JVM client, connect, and run a
  small Kotlin flow
- [Artifact Registration](guides/artifact-registration.md) — how artifacts are
  discovered and registered, and how to narrow or turn that off
- [Spring Boot](guides/spring-boot.md) — the starter, multi-tenancy, and
  per-request identity, causation and units of work
- [Reference](reference/) — annotations, configuration, and service API details
