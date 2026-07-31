# Get Started

By the end of this guide you will have a Kotlin or Java application
that appends events to Chronicle and reads a projected read model back.
The entire example is a self-contained Gradle project.

## Prerequisites

- JDK 17 or later
- A running Chronicle Kernel (see the [Docker Compose snippet](../index.md))
- Gradle 8+

## 1. Add the dependency

The client is published to Maven Central as `io.cratis:chronicle`.

### Kotlin Setup

<!-- validate: skip -->

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.cratis:chronicle:2.1.1")
}
```

### Java Setup

```groovy
// build.gradle
dependencies {
    implementation 'io.cratis:chronicle:2.1.1'
}
```

## 2. Connect to the kernel

`ChronicleClient` is the entry point, and it takes a `ChronicleOptions`.
For local development use the `development()` factory, which points at
`localhost:35000` over TLS with the standard development credentials:

### Kotlin Development Setup

<!-- validate: body -->

```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions

val client = ChronicleClient(ChronicleOptions.development())
val store = client.getEventStore("MyApp")
```

### Java Development Setup

`ChronicleOptions` is a Kotlin `data class`, so its factory methods are
reached through `Companion` from Java. The `namespace` argument has a
default value in Kotlin only — Java must pass it explicitly.

<!-- validate: body -->

```java
import io.cratis.chronicle.ChronicleClient;
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.EventStore;

var client = new ChronicleClient(ChronicleOptions.Companion.development());
EventStore store = client.getEventStore("MyApp", "Default");
```

For anything other than local development, supply a connection string.
The grammar is `chronicle://<user>:<password>@<host>[:<port>][,<host>...][?<options>]`
— see [Configuration](../reference/configuration.md) for the full set of
hosts, options, and the `chronicle+srv://` form.

### Kotlin Production Setup

<!-- validate: body -->

```kotlin
val client = ChronicleClient(
    ChronicleOptions.fromConnectionString(
        "chronicle://my-client:my-secret@chronicle.internal:35000"
    )
)
```

### Java Production Setup

<!-- validate: body -->

```java
var client = new ChronicleClient(
    ChronicleOptions.Companion.fromConnectionString(
        "chronicle://my-client:my-secret@chronicle.internal:35000"
    )
);
```

## 3. Suspend functions and Java interop

Every call that talks to the kernel — appending, registering, querying —
is a Kotlin `suspend` function. In Kotlin, call them from a coroutine;
`runBlocking` is fine for a console application:

<!-- validate: declarations -->

```kotlin
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val client = ChronicleClient(ChronicleOptions.development())
    val store = client.getEventStore("MyApp")
    // everything below happens inside this coroutine
}
```

Java cannot call `suspend` functions directly. The client ships blocking
bridges in `io.cratis.chronicle.java` — one per service — and the Java
examples below use them:

<!-- validate: declarations -->

```java
import io.cratis.chronicle.java.EventLogJavaBridge;
import io.cratis.chronicle.java.EventTypesServiceJavaBridge;
import io.cratis.chronicle.java.ReactorsServiceJavaBridge;
import io.cratis.chronicle.java.ReadModelsJavaBridge;
import io.cratis.chronicle.java.ReducersServiceJavaBridge;
```

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

## 5. Register the event types

Chronicle needs the schema for an event type before it will accept events
of that type. Register them once at startup, before appending anything.

### Kotlin Event Type Registration

<!-- validate: body needs=store -->

```kotlin
store.eventTypes.register(EmployeeHired::class)
```

### Java Event Type Registration

<!-- validate: body needs=store -->

```java
EventTypesServiceJavaBridge.register(store.getEventTypes(), EmployeeHired.class);
```

## 6. Append an event

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
    println("Failed: ${result.constraintViolations.map { it.message }}")
}
```

### Java Append Event

<!-- validate: body needs=store -->

```java
import java.util.stream.Collectors;

String employeeId = "emp-001";
var result = EventLogJavaBridge.append(
    store.getEventLog(),
    employeeId,
    new EmployeeHired(
        employeeId,
        "Jane",
        "Smith",
        "Engineering"
    ),
    null
);

if (result.isSuccess()) {
    System.out.println("Appended at sequence " +
        EventLogJavaBridge.getSequenceNumber(result));
} else {
    String violations =
        result.getConstraintViolations().stream()
            .map(v -> v.getMessage())
            .collect(Collectors.joining(", "));
    System.out.println("Failed: " + violations);
}
```

`EventSequenceNumber` is a Kotlin value class, so it has no ordinary
getter on the JVM — read the sequence number through
`EventLogJavaBridge.getSequenceNumber` rather than off the result.

## 7. React to events

A reactor observes events and performs side effects (see
[Reactors](/chronicle/reactors/) for the full model). Annotate the
class with `@Reactor` and write one method per event type you want to
handle. The first parameter type is what selects the events a method
receives — the method name is free.

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

Register it to start observing:

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

Register it to start observing:

<!-- validate: body needs=store -->

```java
ReactorsServiceJavaBridge.register(store.getReactors(), new HrNotifications());
```

## 8. Build a read model

A reducer folds a stream of events into a single object (see
[Reducers](/chronicle/reducers/) for the full model). The `@ReadModel`
marks the read model class, and `@Reducer` marks the reducer.

A reducer method takes the event, and optionally the current state. The
state is `null` for the first event of an event source, so declare that
parameter as nullable and fall back to a fresh instance.

Registering a reducer registers its read model too — you only need to
register a read model explicitly when nothing projects into it.

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

<!-- validate: body needs=store -->

```java
ReducersServiceJavaBridge.register(
    store.getReducers(), new EmployeeProfileReducer());
```

## 9. Query a read model by key

After events have been projected, query the read model by its event
source identifier:

### Kotlin Query

<!-- validate: body needs=store,employeeId -->

```kotlin
val profile = store.readModels.getInstanceByKey(
    EmployeeProfile::class,
    employeeId
)
println(profile?.firstName) // Jane
```

### Java Query

<!-- validate: body needs=store,employeeId -->

```java
EmployeeProfile profile = ReadModelsJavaBridge.getInstanceByKey(
    store.getReadModels(),
    EmployeeProfile.class,
    employeeId
);
System.out.println(profile.getFirstName()); // Jane
```

## What's next

- [Guides](../guides/toc.yml) — deeper dives into reactors,
  projections, constraints, seeding, and compliance
- [Concepts](../concepts/toc.yml) — understand events, observers,
  and the read model pipeline
- [Reference](../reference/toc.yml) — full annotation and API
  reference
