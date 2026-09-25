---
title: Chronicle for Kotlin and Java
description: Install and configure the Chronicle JVM client for Kotlin and Java, with requirements, compatibility, and links to the shared Chronicle topics.
---

The Chronicle JVM client lets a Kotlin or Java application append events,
build read models from them, and react to them, with annotations on ordinary
data classes and records. Kotlin gets a coroutine API; Java gets the same
surface as blocking calls. One artifact, `io.cratis:chronicle`, serves both.

New here? Start with [Get started](get-started/index.md).

This section covers what is specific to the JVM: installation, connection
setup, Spring Boot, annotations, and the API reference. Shared Chronicle
concepts and workflows live in the main Chronicle docs and show Kotlin and
Java tabs next to the other clients.

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

You need a running Chronicle kernel. For a first try, the development image
runs on its own:

```shell
docker run --rm -p 127.0.0.1:35000:35000 cratis/chronicle:latest-development
```

For a setup closer to production, with storage in its own container, use
Docker Compose. Chronicle serves gRPC, the API, and health checks on port
`35000` over TLS with a self-signed development certificate. The ports below
are bound to loopback so the development kernel and database are not
reachable from other machines:

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
      - "127.0.0.1:35000:35000"

  mongodb:
    image: mongo:8.2
    command: ["mongod", "--replSet", "rs0", "--bind_ip_all"]
    ports:
      - "127.0.0.1:27017:27017"

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

Then add the client to your Gradle build. The pages in this section were
checked against `6.4.0`; the
[latest release](https://central.sonatype.com/artifact/io.cratis/chronicle) is
on Maven Central:

<!-- validate: skip -->

```kotlin
dependencies {
    implementation("io.cratis:chronicle:6.4.0")
}
```

Maven uses the same coordinates, `io.cratis:chronicle:6.4.0`. The same JVM
client supports both Kotlin and Java. Shared pages include separate Kotlin
and Java tabs when both examples exist.

Spring Boot applications add the starter instead. It brings the client with
it and wires everything up, and it needs one version adjustment described in
the [Spring Boot guide](guides/spring-boot.md#add-the-dependency):

<!-- validate: skip -->

```kotlin
dependencies {
    implementation("io.cratis:chronicle-spring-boot-starter:6.4.0")
}
```

## Requirements and compatibility

- **Client**, `io.cratis:chronicle`: Java 17 or later, and
  `kotlinx-coroutines` 1.11 or later at runtime.
- **Spring Boot starter**, `io.cratis:chronicle-spring-boot-starter`:
  Spring Boot 4 (built against 4.1.1); the per-request features need the
  servlet stack.
- **In-process testing**, `io.cratis:chronicle-testing`: any test runner and
  no kernel.

All three are released together with the same version number. The client
ships with Kotlin 2.4 and brings the Kotlin standard library with it, so a
Java application needs no Kotlin setup.

The client checks compatibility with the kernel every time it connects, and
refuses to send anything to a kernel that cannot serve it. There is no
published version matrix. Client 6.4.0 has been exercised against kernel
19.4.8; treat other pairings as unverified until that check and your own
tests pass. See [Connection lifecycle](reference/connection-lifecycle.md#compatibility).

## Client-specific pages

- [Get Started](get-started/): install the JVM client, connect, append an
  event, and read the read model it produces, in Kotlin and Java
- [Artifact Registration](guides/artifact-registration.md): how artifacts are
  discovered and registered, and how to narrow or turn that off
- [Spring Boot](guides/spring-boot.md): the starter, multi-tenancy, and
  per-request identity, causation and units of work
- [Testing](guides/testing.md): specify appends and reducers in-process,
  without a kernel
- [Connection lifecycle](reference/connection-lifecycle.md): what happens on
  connect, reconnect and failure, and how to troubleshoot it
- [Reference](reference/): annotations, configuration, and service API details
- [All JVM guides](guides/index.md)
