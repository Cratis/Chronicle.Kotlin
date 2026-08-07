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
    implementation("io.cratis:chronicle:2.1.2")
}
```

### Java Setup

```groovy
// build.gradle
dependencies {
    implementation 'io.cratis:chronicle:2.1.2'
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

Java goes through `BlockingChronicleClient`, which is the same client with
the coroutines taken off — see
[step 3](#3-suspend-functions-and-java-interop).

<!-- validate: body -->

```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.java.BlockingChronicleClient;
import io.cratis.chronicle.java.BlockingEventStore;

var client = BlockingChronicleClient.connect(ChronicleOptions.development());
BlockingEventStore store = client.getEventStore("MyApp");
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
var client = BlockingChronicleClient.connect(
    ChronicleOptions.fromConnectionString(
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

Java cannot call a `suspend` function at all — it carries a hidden
continuation on the JVM. So Java uses `BlockingChronicleClient`, the same
client with the waiting done for it:

<!-- validate: declarations -->

```java
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.java.BlockingChronicleClient;

class JavaMain {
    void run() {
        var client = BlockingChronicleClient.connect(ChronicleOptions.development());
        var store = client.getEventStore("MyApp");
        // everything below is an ordinary blocking call
    }
}
```

Each call blocks until the kernel answers, which is what a `main`, a
controller method or a scheduled job wants. `unwrap()` on any of these
returns the suspending object underneath, and
`io.cratis.chronicle.java` also holds lower-level static bridges for the
corners the blocking client does not wrap — see
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

Nothing had to be registered first. Chronicle needs the schema for an event
type before it will accept events of that type, and the same goes for every
other artifact your application owns — but the client finds them all on the
classpath and declares them to the kernel as it connects, and the first
append waits for that to finish. There is no registration step to write and
none to sequence.

Prefer to register by hand? Turn discovery off with
`ChronicleOptions.development().withoutAutoRegistration()` and call the
services yourself — `store.eventTypes.register(EmployeeHired::class)` and
friends. See
[Artifact Registration](../guides/artifact-registration.md) for both paths
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
    println("Failed: ${result.constraintViolations.map { it.message }}")
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
    System.out.println("Failed: " + violations);
}
```

`sequenceNumber` is an `EventSequenceNumber`, a Kotlin value class, whose
getter has a mangled JVM signature Java cannot name — so `AppendResult`
carries `getSequenceNumberValue()` for reading the position as a plain
`long`. The same goes the other way: Java never has to construct a value
class, because nothing Java-facing asks for one.

## 6. React to events

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

The client finds it and starts the observation on connect. To register it
by hand instead:

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

The client finds it and starts the observation on connect. To register it
by hand instead:

<!-- validate: body needs=blockingStore -->

```java
store.getReactors().register(new HrNotifications());
```

## 7. Build a read model

A reducer folds a stream of events into a single object (see
[Reducers](/chronicle/reducers/) for the full model). The `@ReadModel`
marks the read model class, and `@Reducer` marks the reducer.

A reducer method takes the event, and optionally the current state. The
state is `null` for the first event of an event source, so declare that
parameter as nullable and fall back to a fresh instance.

A reducer registers its read model too, tagged with the reducer that
produces it — so a read model only needs registering on its own when
nothing projects into it. All of it happens automatically; the calls
below are what you would write with discovery turned off.

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

<!-- validate: body needs=blockingStore,employeeId -->

```java
EmployeeProfile profile = store.getReadModels()
    .getInstanceByKey(EmployeeProfile.class, employeeId);
System.out.println(profile.getFirstName()); // Jane
```

## What's next

- [Guides](../guides/toc.yml) — deeper dives into reactors,
  projections, constraints, seeding, and compliance
- [Concepts](../concepts/toc.yml) — understand events, observers,
  and the read model pipeline
- [Reference](../reference/toc.yml) — full annotation and API
  reference
