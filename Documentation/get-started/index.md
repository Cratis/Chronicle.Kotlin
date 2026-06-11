# Get Started

By the end of this guide you will have a Kotlin application that appends events to Chronicle and reads a projected read model back. The entire example is a self-contained Gradle project.

## Prerequisites

- JDK 17 or later
- A running Chronicle Kernel (see the [Docker Compose snippet](../index.md))
- Gradle 8+

## 1. Add the dependency

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.cratis:chronicle:0.1.0")
}
```

## 2. Connect to the kernel

`ChronicleClient` is the entry point. For local development use the `development()` factory, which connects to `localhost:35000`:

```kotlin
import io.cratis.chronicle.ChronicleClient

val client = ChronicleClient.development()
val store = client.getEventStore("MyApp")
```

For production, supply explicit options:

```kotlin
val client = ChronicleClient(
    ChronicleOptions(host = "chronicle.internal", port = 35000)
)
```

## 3. Define an event type

Annotate a Kotlin data class with `@EventType`. The `id` is a stable string identifier for the event type across deployments.

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

## 4. Append an event

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

## 5. React to events

A reactor observes events and performs side effects. Annotate the class with `@Reactor` and write one method per event type you want to handle.

```kotlin
import io.cratis.chronicle.observation.Reactor

@Reactor
class HrNotifications {
    fun onEmployeeHired(event: EmployeeHired) {
        println("Welcome ${event.firstName} ${event.lastName} to ${event.department}!")
    }
}

// Register and start observing
store.reactors.register(HrNotifications())
```

## 6. Build a read model

A reducer folds a stream of events into a single mutable object. The `@ReadModel` marks the read model class, and `@Reducer` marks the reducer.

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

## 7. Query a read model by key

After events have been projected, query the read model by its event source identifier:

```kotlin
val profile = store.readModels.getInstanceByKey(EmployeeProfile::class, employeeId)
println(profile?.firstName) // Jane
```

## What's next

- [Guides](../guides/toc.yml) — deeper dives into reactors, projections, constraints, seeding, and compliance
- [Concepts](../concepts/toc.yml) — understand events, observers, and the read model pipeline
- [Reference](../reference/toc.yml) — full annotation and API reference
