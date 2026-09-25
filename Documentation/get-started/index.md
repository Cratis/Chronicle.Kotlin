---
title: Get started
description: Add the Chronicle JVM client to a Kotlin or Java project, connect to a local kernel, append an event, and read the resulting read model.
---

You have a Kotlin or Java service and want its state to come from a record of
facts instead of rows that get overwritten. This page takes you from an empty
Gradle project to an application that appends an `EmployeeHired` event, reacts
to it, and reads the `EmployeeProfile` it produces, in both Kotlin and Java.

The steps are excerpts of one program. Put the declarations in your source set,
and run the calls in order from one `main` function (Kotlin) or one `main`
method (Java).

## Prerequisites

- JDK 17 or later. The client is compiled for Java 17.
- Gradle 8 or later, or Maven.
- A running Chronicle kernel on `localhost:35000`. For the quickest start, run
  the development image, which bundles its own storage and binds only to
  loopback here:

  ```shell
  docker run --rm -p 127.0.0.1:35000:35000 cratis/chronicle:latest-development
  ```

  The kernel is ready when `curl -sk https://localhost:35000/health` prints
  `Healthy`. See the [overview](../index.md) for a Docker Compose setup with
  separate storage.

## 1. Add the dependency

The client is published to Maven Central as `io.cratis:chronicle`. The examples
on this page were checked against `6.5.0`; use the
[latest release](https://central.sonatype.com/artifact/io.cratis/chronicle) for
a new project.

### Kotlin Setup

<!-- validate: skip -->

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.cratis:chronicle:6.5.0")
}
```

### Java Setup

```groovy
// build.gradle
dependencies {
    implementation 'io.cratis:chronicle:6.5.0'
}
```

With Maven:

```xml
<dependency>
    <groupId>io.cratis</groupId>
    <artifactId>chronicle</artifactId>
    <version>6.5.0</version>
</dependency>
```

The client brings the Kotlin standard library and `kotlinx-coroutines` with it,
so a Java project needs nothing else. It runs on `kotlinx-coroutines` 1.10.2 or
later, which includes the version Spring Boot 4.1 manages.

## 2. Connect to the kernel

`ChronicleClient` is the entry point, and it takes a `ChronicleOptions`.
For local development use the `development()` factory, which points at
`localhost:35000` over TLS with the kernel's development credentials:

### Kotlin Development Setup

<!-- validate: body -->

```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions

val client = ChronicleClient(ChronicleOptions.development())
val store = client.getEventStore("MyApp")
```

### Java Development Setup

Java goes through `BlockingChronicleClient`, which is the same client with
the coroutines taken off. See
[step 3](#3-suspend-functions-and-java-interop).

<!-- validate: body -->

```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.java.BlockingChronicleClient;
import io.cratis.chronicle.java.BlockingEventStore;

var client = BlockingChronicleClient.connect(ChronicleOptions.development());
BlockingEventStore store = client.getEventStore("MyApp");
```

Creating the client opens the connection straight away and checks that the
kernel speaks a compatible protocol. If the kernel is unreachable, or
incompatible, the constructor (or `connect`) throws, typically an
`io.grpc.StatusException` with status `UNAVAILABLE`. After that first
connection the client reconnects on its own. See
[Connection lifecycle](../reference/connection-lifecycle.md) for what happens
when.

`development()` skips certificate validation so that it accepts the kernel's
self-signed development certificate. Use it only against a kernel on your own
machine.

For anything else, supply a connection string and turn certificate validation
on. The grammar is
`chronicle://<user>:<password>@<host>[:<port>][,<host>...][?<options>]`; see
[Configuration](../reference/configuration.md) for the full set of hosts,
options, and the `chronicle+srv://` form. Keep the secret out of source code.
The connection string is not URL-decoded and everything after the first `?`
is read as options, so a secret containing `?` needs the
`ChronicleConnectionString` constructor instead; see
[Configuration](../reference/configuration.md#tls-and-authentication):

### Kotlin Production Setup

<!-- validate: body -->

```kotlin
val secret = System.getenv("CHRONICLE_CLIENT_SECRET")
    ?: error("Set CHRONICLE_CLIENT_SECRET to the secret for my-client")
val client = ChronicleClient(
    ChronicleOptions.fromConnectionString(
        "chronicle://my-client:$secret@chronicle.internal:35000" +
            "?skipTlsValidation=false"
    )
)
```

### Java Production Setup

<!-- validate: body -->

```java
String secret = System.getenv("CHRONICLE_CLIENT_SECRET");
if (secret == null) {
    throw new IllegalStateException(
        "Set CHRONICLE_CLIENT_SECRET to the client secret for my-client");
}
var client = BlockingChronicleClient.connect(
    ChronicleOptions.fromConnectionString(
        "chronicle://my-client:" + secret + "@chronicle.internal:35000"
            + "?skipTlsValidation=false"
    )
);
```

:::danger[Certificate validation is off unless you turn it on]
Every connection string defaults to `skipTlsValidation=true`, not only the
development one. Without `?skipTlsValidation=false`, the client accepts any
server certificate and sends its credentials to whoever answers.
:::

## 3. Suspend functions and Java interop

Every call that talks to the kernel is a Kotlin `suspend` function. That covers
appending, registering and querying. In Kotlin, call them from a coroutine;
`runBlocking` is fine for a console application:

<!-- validate: declarations -->

```kotlin
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = ChronicleClient(ChronicleOptions.development())
    val store = client.getEventStore("MyApp")
    // everything below happens inside this coroutine
    client.dispose()
}
```

Java cannot call a `suspend` function at all, because on the JVM it carries a
hidden continuation parameter. So Java uses `BlockingChronicleClient`, the same
client with the waiting done for it:

<!-- validate: declarations -->

```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.java.BlockingChronicleClient;

class JavaMain {
    void run() {
        var options = ChronicleOptions.development();
        try (var client = BlockingChronicleClient.connect(options)) {
            var store = client.getEventStore("MyApp");
            // everything below is an ordinary blocking call
        }
    }
}
```

Each call blocks its thread until the kernel answers, which suits a `main`
method, a servlet controller or a scheduled job. Do not call the blocking
client from inside a coroutine; Kotlin code should use the suspending API.
`unwrap()` on any blocking object returns the suspending object underneath,
and `io.cratis.chronicle.java` also holds lower-level static bridges for the
corners the blocking client does not wrap. See
[Java interop](../reference/event-store-api.md#java-interop).

## 4. Define an event type

Annotate a data class or Java record with `@EventType`. The class name is
used as the identifier, so no argument is needed.

### Kotlin Event Definition

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class EmployeeHired(
    val employeeId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val department: String = ""
)
```

### Java Event Definition

<!-- validate: declarations -->

```java
import io.cratis.chronicle.events.EventType;

@EventType
public record EmployeeHired(
    String employeeId,
    String firstName,
    String lastName,
    String department
) {}
```

## 5. Append an event

You do not register anything first. Chronicle needs the schema for an event
type before it accepts events of that type, and the same goes for every other
artifact your application owns. The client finds them on the classpath and
declares them to the kernel as it connects, and the first append waits for that
first registration pass to finish.

Prefer to register by hand? Turn discovery off with
`ChronicleOptions.development().withoutAutoRegistration()` and call the
services yourself, for example `store.eventTypes.register(EmployeeHired::class)`.
See [Artifact Registration](../guides/artifact-registration.md) for both paths
in full.

### Kotlin Append Event

<!-- validate: body needs=store -->

```kotlin
val employeeId = "emp-001"
val result = store.eventLog.append(
    eventSourceId = employeeId,
    event = EmployeeHired(
        employeeId = employeeId,
        firstName = "Jane",
        lastName = "Smith",
        department = "Engineering"
    )
)

if (result.isSuccess) {
    println("Appended at sequence ${result.sequenceNumber.value}")
} else {
    println("Failed: ${result.constraintViolations.map { it.message }} ${result.errors}")
}
```

### Java Append Event

<!-- validate: body needs=blockingStore -->

```java
import java.util.stream.Collectors;

String employeeId = "emp-001";
var result = store.getEventLog().append(
    employeeId,
    new EmployeeHired(
        employeeId,
        "Jane",
        "Smith",
        "Engineering"
    )
);

if (result.isSuccess()) {
    System.out.println("Appended at sequence " +
        result.getSequenceNumberValue());
} else {
    String violations =
        result.getConstraintViolations().stream()
            .map(v -> v.getMessage())
            .collect(Collectors.joining(", "));
    System.out.println("Failed: " + violations + " " + result.getErrors());
}
```

A constraint or concurrency violation does not throw: `isSuccess` is `false`,
and the result carries the violations or errors. Two kinds of failure do
throw. When the kernel refuses the command itself, for example because the
client is not authorized, the client throws `ChronicleCommandRejected`. A
transport failure, such as a lost connection, is thrown as a gRPC
`StatusException`.

`sequenceNumber` is an `EventSequenceNumber`, a Kotlin value class whose
getter has a mangled JVM signature Java cannot name. That is why `AppendResult`
carries `getSequenceNumberValue()` for reading the position as a plain `long`.
Java never has to construct a value class either, because nothing Java-facing
asks for one.

## 6. React to events

A reactor observes events and performs side effects (see
[Reactors](/chronicle/reactors/) for the full model). Annotate the
class with `@Reactor` and write one method per event type you want to
handle. The first parameter type is what selects the events a method
receives; the method name is free.

### Kotlin Reactor

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.observation.Reactor

@Reactor
class HrNotifications {
    fun employeeHired(event: EmployeeHired) {
        println("Welcome ${event.firstName} ${event.lastName} " +
                "to ${event.department}!")
    }
}
```

The client finds it and starts the observation on connect. With discovery
turned off, register it by hand instead. Do not do both: every `register`
call opens its own observation stream for the reactor.

<!-- validate: body needs=store -->

```kotlin
store.reactors.register(HrNotifications())
```

### Java Reactor

<!-- validate: declarations -->

```java
import io.cratis.chronicle.observation.Reactor;

@Reactor
public class HrNotifications {
    public void employeeHired(EmployeeHired event) {
        System.out.println("Welcome " + event.firstName() +
                          " " + event.lastName() +
                          " to " + event.department() + "!");
    }
}
```

The client finds it and starts the observation on connect. With discovery
turned off, register it by hand instead:

<!-- validate: body needs=blockingStore -->

```java
store.getReactors().register(new HrNotifications());
```

The reactor runs after the append has been stored, on its own schedule. The
`Welcome` line can appear before or after the next line of your program.

## 7. Build a read model

A reducer folds a stream of events into a single object (see
[Reducers](/chronicle/reducers/) for the full model). `@ReadModel` marks the
read model class, and `@Reducer` marks the reducer.

A reducer method takes the event, and optionally the current state. The
state is `null` for the first event of an event source, so declare that
parameter as nullable and fall back to a fresh instance.

A reducer registers its read model too, tagged with the reducer that
produces it, so a read model only needs registering on its own when
nothing projects into it. All of it happens automatically; the `register`
calls below are what you would write with discovery turned off.

### Kotlin Read Model

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.readModels.ReadModel
import io.cratis.chronicle.observation.Reducer

@ReadModel
data class EmployeeProfile(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val department: String = ""
)

@Reducer
class EmployeeProfileReducer {
    fun employeeHired(
        event: EmployeeHired,
        state: EmployeeProfile?
    ): EmployeeProfile =
        (state ?: EmployeeProfile()).copy(
            id = event.employeeId,
            firstName = event.firstName,
            lastName = event.lastName,
            department = event.department
        )
}
```

<!-- validate: body needs=store -->

```kotlin
store.reducers.register(EmployeeProfileReducer())
```

### Java Read Model

Java needs one file per public type, so the read model and the reducer are
two files.

<!-- validate: declarations -->

```java
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
public class EmployeeProfile {
    private String id = "";
    private String firstName = "";
    private String lastName = "";
    private String department = "";

    public EmployeeProfile() {}

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) {
        this.department = department;
    }
}
```

<!-- validate: declarations -->

```java
import io.cratis.chronicle.observation.Reducer;

@Reducer
public class EmployeeProfileReducer {
    public EmployeeProfile employeeHired(EmployeeHired event,
                                         EmployeeProfile state) {
        EmployeeProfile result =
            state != null ? state : new EmployeeProfile();
        result.setId(event.employeeId());
        result.setFirstName(event.firstName());
        result.setLastName(event.lastName());
        result.setDepartment(event.department());
        return result;
    }
}
```

<!-- validate: body needs=blockingStore -->

```java
store.getReducers().register(new EmployeeProfileReducer());
```

## 8. Query a read model by key

The reducer runs in the background after the append is stored, and its result
is written to the read model store a moment later. So a read straight after the
append can come back `null`, or with the state from before the event. A
successful append means the event is stored, not that every read model has
caught up with it.

For this walkthrough, wait for the instance with an explicit time limit, and
fail clearly when it does not appear:

### Kotlin Query

<!-- validate: body needs=store,employeeId -->

```kotlin
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

val profile = withTimeoutOrNull(10_000) {
    var current = store.readModels.getInstanceByKey(EmployeeProfile::class, employeeId)
    while (current == null) {
        delay(100)
        current = store.readModels.getInstanceByKey(EmployeeProfile::class, employeeId)
    }
    current
} ?: error("EmployeeProfile $employeeId was not available within 10 seconds")

println(profile.firstName) // Jane
```

### Java Query

<!-- validate: body needs=blockingStore,employeeId -->

```java
import java.time.Duration;
import java.time.Instant;

Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
EmployeeProfile profile = store.getReadModels()
    .getInstanceByKey(EmployeeProfile.class, employeeId);
while (profile == null && Instant.now().isBefore(deadline)) {
    Thread.sleep(100);
    profile = store.getReadModels()
        .getInstanceByKey(EmployeeProfile.class, employeeId);
}
if (profile == null) {
    throw new IllegalStateException(
        "EmployeeProfile " + employeeId + " was not available within 10 seconds");
}
System.out.println(profile.getFirstName()); // Jane
```

The Kotlin `withTimeoutOrNull` also cancels a read that is still in flight.
The Java loop only checks its deadline between reads, and a single blocking
read has no time limit of its own, so it can overrun when the kernel stops
answering.

This loop waits for the instance to exist. When you are waiting for a change
to an instance that already exists, check for the value you expect instead of
`null`.

Polling after every write is a teaching device, not an application pattern.
In an application, return the append result to the caller and let screens
observe the read model, or design the next step so it does not depend on the
read model having caught up. When a read must include the latest events, make
the reducer passive with `@Reducer(isActive = false)` (see
[`@Reducer`](../reference/annotations.md#reducer)): the read model is then
computed from its events on every read instead of stored. The shared
[read model consistency](/chronicle/read-models/) docs explain the trade-offs.

## Troubleshooting

**The constructor or `connect` throws `StatusException: UNAVAILABLE`.**
The kernel is not running, not on that host and port, or the TLS settings do
not match it. Check `curl -sk https://localhost:35000/health`. With
`skipTlsValidation=false`, make sure the JVM trusts the kernel's certificate.

**The first append or `awaitRegistration()` throws `ChronicleConnectionFailed`
mentioning `UNAUTHENTICATED`.** The client id or secret is wrong. Fix the
credentials; the client keeps retrying and recovers once they are accepted.

**stderr shows `Automatic registration of artifacts failed`.** An artifact
could not be registered or created, and the artifacts after it in the
registration order were not registered. See
[Artifact Registration](../guides/artifact-registration.md#when-registration-fails).

**The read model is `null` right after appending.** It has not caught up yet.
That is expected; wait with a time limit, as in step 8.

## What's next

- [Guides](../guides/index.md): Spring Boot, artifact registration, testing,
  seeding, and the other JVM-specific topics
- [Concepts](../concepts/index.md): where the shared explanations of events,
  observers and read models live
- [Reference](../reference/index.md): annotations, the `IEventStore` API,
  configuration, and the connection lifecycle
