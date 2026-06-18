# Get Started

By the end of this guide you will have a Kotlin or Java application
that appends events to Chronicle and reads a projected read model back.
The entire example is a self-contained Gradle project.

## Prerequisites

- JDK 17 or later
- A running Chronicle Kernel (see the [Docker Compose snippet](../index.md))
- Gradle 8+

## 1. Add the dependency

### Kotlin Setup

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.cratis:chronicle:0.1.0")
}
```

### Java Setup

```groovy
// build.gradle
dependencies {
    implementation 'io.cratis:chronicle:0.1.0'
}
```

## 2. Connect to the kernel

`ChronicleClient` is the entry point. For local development use the
`development()` factory, which connects to `localhost:35000`:

### Kotlin Development Setup

```kotlin
import io.cratis.chronicle.ChronicleClient

val client = ChronicleClient.development()
val store = client.getEventStore("MyApp")
```

### Java Development Setup

```java
import io.cratis.chronicle.ChronicleClient;

var client = new ChronicleClient(ChronicleOptions.Companion.development());
var store = client.getEventStore("MyApp");
```

For production, supply explicit options:

### Kotlin Production Setup

```kotlin
val client = ChronicleClient(
    ChronicleOptions(host = "chronicle.internal", port = 35000)
)
```

### Java Production Setup

```java
var client = new ChronicleClient(
    new ChronicleOptions("chronicle.internal", 35000)
);
```

## 3. Define an event type

Annotate a data class or Java class with `@EventType`.

### Kotlin Event Definition

The `id` is a stable string identifier for the event type across deployments.

```kotlin
import io.cratis.chronicle.events.EventType

@EventType(id = "EmployeeHired")
data class EmployeeHired(
    val employeeId: String,
    val firstName: String,
    val lastName: String,
    val department: String
)
```

### Java Event Definition

```java
import io.cratis.chronicle.events.EventType;

@EventType
public class EmployeeHired {
    private String employeeId;
    private String firstName;
    private String lastName;
    private String department;

    public EmployeeHired() {}

    public EmployeeHired(String employeeId, String firstName,
                         String lastName, String department) {
        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.department = department;
    }

    // Getters and setters
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

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

## 4. Append an event

### Kotlin Append Event

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

```java
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
        result.getSequenceNumber().getValue());
} else {
    String violations =
        result.getConstraintViolations().stream()
            .map(v -> v.getMessage())
            .collect(Collectors.joining(", "));
    System.out.println("Failed: " + violations);
}
```

## 5. React to events

A reactor observes events and performs side effects. Annotate the
class with `@Reactor` and write one method per event type you want to
handle.

### Kotlin Reactor

```kotlin
import io.cratis.chronicle.observation.Reactor

@Reactor
class HrNotifications {
    fun onEmployeeHired(event: EmployeeHired) {
        println("Welcome ${event.firstName} ${event.lastName} " +
                "to ${event.department}!")
    }
}

// Register and start observing
store.reactors.register(HrNotifications())
```

### Java Reactor

```java
import io.cratis.chronicle.observation.Reactor;

@Reactor
public class HrNotifications {
    public void onEmployeeHired(EmployeeHired event) {
        System.out.println("Welcome " + event.getFirstName() +
                          " " + event.getLastName() +
                          " to " + event.getDepartment() + "!");
    }
}

// Register and start observing
store.getReactors().register(new HrNotifications());
```

## 6. Build a read model

A reducer folds a stream of events into a single mutable object. The
`@ReadModel` marks the read model class, and `@Reducer` marks the reducer.

### Kotlin Read Model

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
    fun on(event: EmployeeHired, state: EmployeeProfile): EmployeeProfile =
        state.copy(
            id = event.employeeId,
            firstName = event.firstName,
            lastName = event.lastName,
            department = event.department
        )
}

store.reducers.register(EmployeeProfileReducer())
```

### Java Read Model

```java
import io.cratis.chronicle.readModels.ReadModel;
import io.cratis.chronicle.observation.Reducer;

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

@Reducer
public class EmployeeProfileReducer {
    public EmployeeProfile on(EmployeeHired event,
                              EmployeeProfile state) {
        EmployeeProfile result =
            state != null ? state : new EmployeeProfile();
        result.setId(event.getEmployeeId());
        result.setFirstName(event.getFirstName());
        result.setLastName(event.getLastName());
        result.setDepartment(event.getDepartment());
        return result;
    }
}

store.getReducers().register(new EmployeeProfileReducer());
```

## 7. Query a read model by key

After events have been projected, query the read model by its event
source identifier:

### Kotlin Query

```kotlin
val profile = store.readModels.getInstanceByKey(
    EmployeeProfile::class,
    employeeId
)
println(profile?.firstName) // Jane
```

### Java Query

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
