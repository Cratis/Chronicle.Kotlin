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
Source/              ← io.cratis:chronicle Kotlin library
Documentation/       ← User-facing documentation
Samples/
  Kotlin/Console/    ← Kotlin console sample application
  Java/Console/      ← Java console sample application
```

## Prerequisite: Chronicle Running

You need a Chronicle Kernel available before running samples or application code.

The easiest local setup is the development Docker image:

```bash
docker run -p 35000:35000 -p 8080:8080 cratis/chronicle:latest-development
```

## Getting Started

### Kotlin

Add the dependency to your Gradle build:

```kotlin
dependencies {
    implementation("io.cratis:chronicle:<version>")
}
```

### Java

Add the dependency to your Gradle build:

```groovy
dependencies {
    implementation 'io.cratis:chronicle:<version>'
}
```

## Quick Example

### Kotlin

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

### Java

```java
import io.cratis.chronicle.ChronicleClient;
import io.cratis.chronicle.ChronicleOptions;
import io.cratis.chronicle.events.EventType;

@EventType
public class EmployeeHired {
    private String firstName;
    private String lastName;
    private String title;
    
    public EmployeeHired(String firstName, String lastName, String title) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.title = title;
    }
    
    // Getters and setters omitted for brevity
}

public class Main {
    public static void main(String[] args) throws Exception {
        ChronicleClient client = new ChronicleClient(ChronicleOptions.Companion.development());
        var store = client.getEventStore("MyStore");
        var result = store.getEventLog().append("employee-123", 
            new EmployeeHired("Jane", "Doe", "Engineer"));
        System.out.println("Appended at sequence number " + result.getSequenceNumber().getValue());
        client.dispose();
    }
}
```

## Reactors

Reactors observe events and produce side effects (notifications, commands to other contexts, etc.):

### Kotlin

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

### Java

```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.observation.Reactor;

@Reactor
public class HrNotificationReactor {
    public void employeeHired(EmployeeHired event, EventContext context) {
        System.out.println("Employee hired: " + event.getFirstName() + " " + event.getLastName());
    }
}

// Register with the event store:
store.getReactors().register(new HrNotificationReactor());
```

## Reducers

Reducers fold events into read models:

### Kotlin

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

### Java

```java
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@ReadModel
public class EmployeeState {
    private String firstName = "";
    private String title = "";
    
    // Constructors, getters, and setters omitted for brevity
}

@Reducer
public class EmployeeStateReducer {
    public EmployeeState employeeHired(EmployeeHired event) {
        return new EmployeeState(event.getFirstName(), event.getTitle());
    }

    public EmployeeState employeePromoted(EmployeePromoted event, EmployeeState state) {
        EmployeeState result = state != null ? state : new EmployeeState();
        result.setTitle(event.getNewTitle());
        return result;
    }
}

// Register and query:
store.getReducers().register(new EmployeeStateReducer());
EmployeeState state = store.getReadModels().getInstanceByKey(EmployeeState.class, "employee-123");
```

## Projections

Declarative projections map events to read models:

### Kotlin

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

### Java

```java
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;
import io.cratis.chronicle.projections.Projection;

@Projection
public class EmployeeProjection implements IProjectionFor<EmployeeState> {
    @Override
    public void define(IProjectionBuilderFor<EmployeeState> builder) {
        builder
            .from(EmployeeHired.class)
            .from(EmployeePromoted.class, fb -> 
                fb.set(EmployeeState::getTitle).toProperty("newTitle")
            );
    }
}

store.getProjections().register(new EmployeeProjection());
```

## Constraints

Prevent invalid state via kernel-enforced constraints:

### Kotlin

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

### Java

```java
import io.cratis.chronicle.constraints.Constraint;
import io.cratis.chronicle.constraints.IConstraint;
import io.cratis.chronicle.constraints.IConstraintBuilder;

@Constraint
public class UniqueEmployeeHire implements IConstraint {
    @Override
    public void define(IConstraintBuilder builder) {
        builder.uniqueFor(EmployeeHired.class, "An employee can only be hired once.");
    }
}

store.getConstraints().register(new UniqueEmployeeHire());
```

## Transactions (Unit of Work)

Stage multiple appends and commit atomically:

### Kotlin

```kotlin
val unitOfWork = store.unitOfWorkManager.begin()
store.eventLog.transactional.append(id1, EmployeePromoted("Senior Engineer"))
store.eventLog.transactional.append(id2, EmployeePromoted("Principal Engineer"))
unitOfWork.commit()
```

### Java

```java
var unitOfWork = store.getUnitOfWorkManager().begin();
store.getEventLog().getTransactional().append(id1, new EmployeePromoted("Senior Engineer"));
store.getEventLog().getTransactional().append(id2, new EmployeePromoted("Principal Engineer"));
unitOfWork.commit();
```

## Seeding

Seed initial events on first startup:

### Kotlin

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

### Java

```java
import io.cratis.chronicle.seeding.ICanSeedEvents;
import io.cratis.chronicle.seeding.IEventSeedingBuilder;
import io.cratis.chronicle.seeding.Seeder;

import java.util.Arrays;

@Seeder
public class EmployeeSeeder implements ICanSeedEvents {
    @Override
    public void seed(IEventSeedingBuilder builder) {
        builder.forEventSource("emp-1", 
            Arrays.asList(new EmployeeHired("Ada", "Lovelace", "Engineer")));
    }
}

store.getSeeding().seed(new EmployeeSeeder());
```

## Building

```bash
gradle :Source:build
```

## Running the Console Sample

Samples are available in both Kotlin and Java. See [Samples/Kotlin/Console/README.md](./Samples/Kotlin/Console/README.md) or [Samples/Java/Console/README.md](./Samples/Java/Console/README.md) for full instructions.

### Kotlin Sample

```bash
docker run -p 35000:35000 -p 8080:8080 cratis/chronicle:latest-development
gradle :Samples:Kotlin:Console:run
```

### Java Sample

```bash
docker run -p 35000:35000 -p 8080:8080 cratis/chronicle:latest-development
gradle :Samples:Java:Console:run
```
