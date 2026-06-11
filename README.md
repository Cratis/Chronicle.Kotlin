# Chronicle Kotlin Client

A Kotlin-idiomatic client for [Cratis Chronicle](https://github.com/Cratis/Chronicle).

## Overview

`io.cratis:chronicle` provides a clean, type-safe Kotlin API for interacting with the Chronicle Kernel. It builds on top of `io.cratis:chronicle-contracts` (the gRPC contracts artifact) and exposes idiomatic Kotlin constructs including:

- **Annotations** — `@EventType`, `@Reactor`, `@Reducer`, `@Projection`, `@ReadModel`, `@Seeder`, `@Constraint`, and `@Pii`
- **Value objects** — `EventSequenceNumber`, `EventTypeId`, `EventStoreName`, etc.
- **Fluent client** — `ChronicleClient` → `EventStore` → `EventLog` → `append()`
- **Coroutines-first** — all async operations use Kotlin coroutines and `Flow`

## Structure

```text
Source/          ← io.cratis:chronicle Kotlin library
Documentation/   ← User-facing documentation
Samples/
  Console/       ← Kotlin console sample application
```

## Prerequisite: Chronicle Running

You need a Chronicle Kernel available before running samples or application code.

The easiest local setup is the development Docker image:

```bash
docker run -p 35000:35000 -p 8080:8080 cratis/chronicle:latest-development
```

## Getting Started

Add the dependency to your Gradle build:

```kotlin
dependencies {
    implementation("io.cratis:chronicle:<version>")
}
```

## Quick Example

```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.events.EventType

@EventType(id = "EmployeeHired")
data class EmployeeHired(val firstName: String, val lastName: String, val title: String)

suspend fun main() {
    val client = ChronicleClient(ChronicleOptions.development())
    val store = client.getEventStore("MyStore")
    val result = store.eventLog.append("employee-123", EmployeeHired("Jane", "Doe", "Engineer"))
    println("Appended at sequence number ${result.sequenceNumber.value}")
    client.dispose()
}
```

## Reactors

Reactors observe events and produce side effects (notifications, commands to other contexts, etc.):

```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reactor

@Reactor
class HrNotificationReactor {
    fun employeeHired(event: EmployeeHired, context: EventContext) {
        println("Employee hired: ${event.firstName} ${event.lastName}")
    }
}

// Register with the event store:
store.reactors.register(HrNotificationReactor())
```

## Reducers

Reducers fold events into read models:

```kotlin
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class EmployeeState(val firstName: String = "", val title: String = "")

@Reducer
class EmployeeStateReducer {
    fun employeeHired(event: EmployeeHired): EmployeeState =
        EmployeeState(firstName = event.firstName, title = event.title)

    fun employeePromoted(event: EmployeePromoted, state: EmployeeState?): EmployeeState =
        (state ?: EmployeeState()).copy(title = event.newTitle)
}

// Register and query:
store.reducers.register(EmployeeStateReducer())
val state = store.readModels.getInstanceByKey(EmployeeState::class, "employee-123")
```

## Projections

Declarative projections map events to read models:

```kotlin
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import io.cratis.chronicle.projections.Projection

@Projection(readModel = EmployeeState::class)
class EmployeeProjection : IProjectionFor<EmployeeState> {
    override fun define(builder: IProjectionBuilderFor<EmployeeState>) {
        builder
            .from(EmployeeHired::class)
            .from(EmployeePromoted::class) { fb ->
                fb.set(EmployeeState::title).toProperty("newTitle")
            }
    }
}

store.projections.register(EmployeeProjection())
```

## Constraints

Prevent invalid state via kernel-enforced constraints:

```kotlin
import io.cratis.chronicle.constraints.Constraint
import io.cratis.chronicle.constraints.IConstraint
import io.cratis.chronicle.constraints.IConstraintBuilder

@Constraint
class UniqueEmployeeHire : IConstraint {
    override fun define(builder: IConstraintBuilder) {
        builder.uniqueFor(EmployeeHired::class, "An employee can only be hired once.")
    }
}

store.constraints.register(UniqueEmployeeHire())
```

## Transactions (Unit of Work)

Stage multiple appends and commit atomically:

```kotlin
val unitOfWork = store.unitOfWorkManager.begin()
store.eventLog.transactional.append(id1, EmployeePromoted("Senior Engineer"))
store.eventLog.transactional.append(id2, EmployeePromoted("Principal Engineer"))
unitOfWork.commit()
```

## Seeding

Seed initial events on first startup:

```kotlin
import io.cratis.chronicle.seeding.ICanSeedEvents
import io.cratis.chronicle.seeding.IEventSeedingBuilder
import io.cratis.chronicle.seeding.Seeder

@Seeder
class EmployeeSeeder : ICanSeedEvents {
    override fun seed(builder: IEventSeedingBuilder) {
        builder.forEventSource("emp-1", listOf(EmployeeHired("Ada", "Lovelace", "Engineer")))
    }
}

store.seeding.seed(EmployeeSeeder())
```

## Building

```bash
gradle :Source:build
```

## Running the Console Sample

See [Samples/Console/README.md](./Samples/Console/README.md) for full instructions.

```bash
docker run -p 35000:35000 -p 8080:8080 cratis/chronicle:latest-development
gradle :Samples:Console:run
```
