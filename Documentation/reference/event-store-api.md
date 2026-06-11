# EventStore API

## IChronicleClient

Entry point for the library.

```kotlin
interface IChronicleClient {
    fun getEventStore(name: String): IEventStore
    fun getEventStore(name: String, namespace: String): IEventStore
    fun close()
}
```

Factory functions:

```kotlin
ChronicleClient.development()                          // localhost:35000
ChronicleClient(options: ChronicleOptions)             // custom host/port
```

---

## IEventStore

```kotlin
interface IEventStore {
    val eventLog: IEventLog
    val reactors: IReactorsService
    val reducers: IReducersService
    val projections: IProjectionsService
    val constraints: IConstraintsService
    val seeding: IEventSeedingService
    val readModels: IReadModelsService
    val compliance: IComplianceService
    val unitOfWork: UnitOfWorkManager
}
```

---

## IEventLog

```kotlin
interface IEventLog {
    suspend fun append(eventSourceId: String, event: Any): AppendResult
    suspend fun append(eventSourceId: String, event: Any, options: AppendOptions): AppendResult
    suspend fun appendMany(eventSourceId: String, events: List<Any>): List<AppendResult>
}
```

### AppendResult

| Property | Type | Description |
|---|---|---|
| `isSuccess` | `Boolean` | `true` when appended without constraint violations |
| `sequenceNumber` | `EventSequenceNumber` | Position in the event log |
| `constraintViolations` | `List<ConstraintViolation>` | Violations when `isSuccess` is `false` |

---

## IReactorsService

```kotlin
interface IReactorsService {
    suspend fun register(reactor: Any): Job
}
```

---

## IReducersService

```kotlin
interface IReducersService {
    suspend fun register(reducer: Any): Job
}
```

---

## IReadModelsService

```kotlin
interface IReadModelsService {
    suspend fun register(vararg readModelClasses: KClass<*>)
    suspend fun <T : Any> getInstanceByKey(readModelClass: KClass<T>, key: String): T?
}
```

---

## IProjectionsService

```kotlin
interface IProjectionsService {
    suspend fun register(vararg projections: Any)
}
```

---

## IConstraintsService

```kotlin
interface IConstraintsService {
    suspend fun register(vararg constraints: Any)
}
```

---

## IEventSeedingService

```kotlin
interface IEventSeedingService {
    suspend fun seed(vararg seeders: Any)
}
```

---

## IComplianceService

```kotlin
interface IComplianceService {
    suspend fun release(subject: String, schema: String, payload: String): String
}
```
