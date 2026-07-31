# EventStore API

## IChronicleClient

Entry point for the library.

<!-- validate: skip -->

```kotlin
interface IChronicleClient : AutoCloseable {
    fun getEventStore(
        name: String,
        namespace: String = EventStoreNamespaceName.default.value
    ): EventStore

    fun dispose()
}
```

Construct it with [ChronicleOptions](configuration.md):

<!-- validate: body -->

```kotlin
// localhost:35000
ChronicleClient(ChronicleOptions.development())

// any server
ChronicleClient(
    ChronicleOptions.fromConnectionString("chronicle://chronicle.internal")
)
```

---

## IEventStore

<!-- validate: skip -->

```kotlin
interface IEventStore {
    val name: String
    val namespace: String
    val eventLog: IEventLog
    val reactors: IReactorsService
    val reducers: IReducersService
    val projections: IProjectionsService
    val constraints: IConstraintsService
    val seeding: IEventSeedingService
    val readModels: IReadModelsService
    val unitOfWorkManager: UnitOfWorkManager
}
```

The concrete `EventStore` returned by `getEventStore` additionally exposes
`compliance`, `eventTypes`, and `namespaces`.

---

## IEventLog

<!-- validate: skip -->

```kotlin
interface IEventLog : IEventSequence {
    val transactional: ITransactionalEventSequence

    suspend fun append(
        eventSourceId: String,
        event: Any,
        options: AppendOptions? = null
    ): AppendResult

    suspend fun appendMany(
        eventSourceId: String,
        events: List<Any>,
        options: AppendOptions? = null
    ): List<AppendResult>

    suspend fun hasEventsFor(eventSourceId: String): Boolean
}
```

### AppendResult

| Property | Type | Description |
| --- | --- | --- |
| `isSuccess` | `Boolean` | `true` when there are no violations or errors |
| `sequenceNumber` | `EventSequenceNumber` | Position in the event log |
| `constraintViolations` | `List<ConstraintViolation>` | On failure |
| `errors` | `List<AppendError>` | On failure |

---

## IReactorsService

<!-- validate: skip -->

```kotlin
interface IReactorsService {
    suspend fun register(reactor: Any): Job
}
```

---

## IReducersService

<!-- validate: skip -->

```kotlin
interface IReducersService {
    suspend fun register(reducer: Any): Job
}
```

---

## IReadModelsService

<!-- validate: skip -->

```kotlin
interface IReadModelsService {
    suspend fun register(vararg readModelClasses: KClass<*>)
    suspend fun <T : Any> getInstanceByKey(
        readModelClass: KClass<T>,
        key: String
    ): T?
}
```

---

## IProjectionsService

<!-- validate: skip -->

```kotlin
interface IProjectionsService {
    suspend fun register(vararg projections: Any)
}
```

---

## IConstraintsService

<!-- validate: skip -->

```kotlin
interface IConstraintsService {
    suspend fun register(vararg constraints: Any)
}
```

---

## IEventSeedingService

<!-- validate: skip -->

```kotlin
interface IEventSeedingService {
    suspend fun seed(vararg seeders: Any)
}
```

---

## IComplianceService

<!-- validate: skip -->

```kotlin
interface IComplianceService {
    suspend fun release(subject: String, schema: String, payload: String): String
}
```

---

## Java interop

Every service method above is a Kotlin `suspend` function and cannot be
called from Java directly. The `io.cratis.chronicle.java` package provides
blocking bridges — each is a static method taking the service as its first
argument.

| Bridge | Wraps |
| --- | --- |
| `EventLogJavaBridge` | `append`, `appendMany`, `hasEventsFor` |
| `TransactionalEventSequenceJavaBridge` | `append`, `appendMany` |
| `EventTypesServiceJavaBridge` | `register` |
| `ReadModelsJavaBridge` | `register`, `getInstanceByKey` |
| `ReactorsServiceJavaBridge` | `register` |
| `ReducersServiceJavaBridge` | `register` |
| `ProjectionsServiceJavaBridge` | `register` |
| `ConstraintsServiceJavaBridge` | `register` |
| `EventSeedingServiceJavaBridge` | `seed` |
| `NamespacesServiceJavaBridge` | `ensure` |
| `UnitOfWorkJavaBridge` | `commit`, `rollback` |

<!-- validate: body needs=store -->

```java
import io.cratis.chronicle.java.EventLogJavaBridge;

var result = EventLogJavaBridge.append(
    store.getEventLog(),
    "emp-001",
    new EmployeeHired("emp-001", "Jane", "Smith", "Engineering"),
    null);
```

`EventSequenceNumber` is a Kotlin value class, so an `AppendResult` has no
ordinary getter for it on the JVM. Read it with
`EventLogJavaBridge.getSequenceNumber(result)`.

`ConstraintBuilderJavaBridge`, `UniqueConstraintBuilderJavaBridge`,
`ProjectionBuilderJavaBridge`, and `CausationManagerJavaBridge` cover the
builder APIs that take a `KClass` in Kotlin, accepting a `Class` instead.
