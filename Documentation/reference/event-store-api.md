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

    suspend fun getEventStores(): List<String>
    fun evictEventStores()
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

`getEventStores` lists every event store known to the kernel — not just the
ones this client has already opened via `getEventStore`. `evictEventStores`
clears this client's internal cache of `EventStore` instances (and their
per-store subscriptions) without disposing the client itself — useful
between test classes that share a client instance.

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
    val compliance: IComplianceService
    val eventTypes: IEventTypesService
    val namespaces: INamespacesService
    val externalServices: IExternalServicesService
    val jobs: IJobsService
    val eventStoreSubscriptions: IEventStoreSubscriptionsService
    val webhooks: IWebhooksService
    val identities: IIdentityManagerService
    val failedPartitions: IFailedPartitions

    fun getEventSequence(id: EventSequenceId): IEventSequence
}
```

Every service is now on the interface itself — `compliance`, `eventTypes`,
`namespaces`, `externalServices`, `jobs`, `eventStoreSubscriptions`,
`webhooks`, and `identities` used to be available only on the concrete
`EventStore` class; code written against `IEventStore` (for example in
tests, behind a fake) now sees the full surface.

`eventLog` is the default event log sequence (`EventSequenceId.eventLog`).
Use `getEventSequence(id)` to get any other, non-default `IEventSequence` by
id — for example one used exclusively by a specific subsystem.

---

## IEventSequence and IEventLog

`IEventLog` is `IEventSequence` plus a `transactional` entry point; both
share the same read/write surface.

<!-- validate: skip -->

```kotlin
interface IEventSequence {
    val id: EventSequenceId
    val appendOperations: SharedFlow<List<AppendedEventWithResult>>

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
    suspend fun appendMany(
        events: List<EventForEventSourceId>,
        concurrencyScopes: Map<String, ConcurrencyScope> = emptyMap(),
        correlationId: UUID? = null
    ): List<AppendResult>
    suspend fun hasEventsFor(eventSourceId: String): Boolean

    suspend fun getTailSequenceNumber(
        eventSourceId: String? = null
    ): EventSequenceNumber
    suspend fun getForEventSourceIdAndEventTypes(
        eventSourceId: String,
        eventTypes: List<KClass<*>>,
        eventStreamType: String? = null,
        eventStreamId: String? = null,
        eventSourceType: String? = null
    ): List<AppendedEvent>
    suspend fun getFromSequenceNumber(
        sequenceNumber: EventSequenceNumber,
        eventSourceId: String? = null,
        eventTypes: List<KClass<*>>? = null
    ): List<AppendedEvent>
    suspend fun getNextSequenceNumber(): EventSequenceNumber
    suspend fun getTailSequenceNumberForObserver(
        observerType: KClass<*>
    ): EventSequenceNumber

    suspend fun completeStream(
        eventStreamType: String,
        eventStreamId: String
    ): CompleteStreamResult

    suspend fun redact(
        sequenceNumber: EventSequenceNumber,
        reason: RedactionReason
    )
    suspend fun redactForEventSource(
        eventSourceId: String,
        reason: RedactionReason,
        eventTypes: List<KClass<*>> = emptyList()
    )
}

interface IEventLog : IEventSequence {
    val transactional: ITransactionalEventSequence
}
```

### Appending across event sources

The `eventSourceId` overload of `appendMany` shapes every event in the
batch the same way. The `List<EventForEventSourceId>` overload instead lets each
event carry its own event source id and its own shaping, so one atomic
batch can span many event sources and many streams. `concurrencyScopes` is
keyed by event source id — only the sources present in the map are
concurrency checked, and any source left out is appended unchecked.

<!-- validate: skip -->

```kotlin
data class EventForEventSourceId(
    val eventSourceId: String,
    val event: Any,
    val eventStreamType: String? = null,
    val eventStreamId: String? = null,
    val eventSourceType: String? = null,
    val tags: List<String> = emptyList(),
    val occurred: Instant? = null,
    val subject: String? = null,
    val causation: List<Causation> = emptyList()
)
```

Every unset field falls back to the same default a plain `append` uses,
resolved against that event's own event source id.

### Composed operations

`IEventSequenceOperations` composes a batch that is decided in more than
one place, then commits it with a single `perform()`. Start one with the
`operations()` or `forEventSourceId(...)` extension on any
`IEventSequence`. Nothing reaches the kernel until `perform()` runs, and
`getEventsToAppend()` shows exactly what it will send.

<!-- validate: skip -->

```kotlin
interface IEventSequenceOperations {
    val eventSequence: IEventSequence

    fun forEventSourceId(
        eventSourceId: String,
        configure: IEventSourceOperations.() -> Unit
    ): IEventSequenceOperations
    fun withCorrelationId(correlationId: UUID): IEventSequenceOperations
    fun withCausation(causation: List<Causation>): IEventSequenceOperations
    fun getAppendedEvents(): List<Any>
    fun getEventsToAppend(): List<EventForEventSourceId>
    fun clear()
    suspend fun perform(): List<AppendResult>
}

interface IEventSourceOperations {
    val operations: List<IEventSequenceOperation>
    val concurrencyScope: ConcurrencyScope

    fun withConcurrencyScope(concurrencyScope: ConcurrencyScope): IEventSourceOperations
    fun withConcurrencyScope(
        configure: ConcurrencyScopeBuilder.() -> Unit
    ): IEventSourceOperations
    fun append(
        event: Any,
        eventStreamType: String? = null,
        eventStreamId: String? = null,
        eventSourceType: String? = null,
        tags: List<String> = emptyList(),
        occurred: Instant? = null,
        subject: String? = null
    ): IEventSourceOperations
    fun <T : IEventSequenceOperation> getOperationsOfType(type: KClass<T>): List<T>
    fun getAppendedEvents(): List<Any>
}
```

Calling `forEventSourceId` twice for the same event source adds to what is
already staged rather than replacing it. The concurrency scope lives on the
event source, since that is where the kernel checks it: an event source
that never sets one is appended unchecked, and a scope already set is never
cleared by a later `ConcurrencyScope.notSet` — so a call that expresses no
expectation cannot silently disable a check that was explicitly asked for.

Causation is not composed here; it is derived from the ambient
`CausationManager` when `perform()` runs.

Java reaches this through `EventSequenceOperationsJavaBridge`
(`operationsFor`, `forEventSourceId`, `perform`) and
`EventSourceOperationsJavaBridge` (`append`, `withConcurrencyScope`), which
supply the blocking calls, `Consumer`-based configuration, and the optional
parameters Kotlin default arguments cannot give Java.

### Reading events back

`getForEventSourceIdAndEventTypes` gets events for one event source,
narrowed to specific event types and (optionally) a specific stream.
`getFromSequenceNumber` instead reads forward from a position in the
sequence, optionally narrowed by event source and/or event type —
useful for catching up from a bookmark. `getTailSequenceNumber` and
`getNextSequenceNumber` report the current end of the sequence (or of one
event source's events within it) and the sequence number the *next*
appended event will receive, respectively. `getTailSequenceNumberForObserver`
reports the tail relative to the event types a specific reactor or reducer
type actually handles (discovered by reflection over its handler methods).

<!-- validate: skip -->

```kotlin
data class AppendedEvent(
    val context: EventContext,
    val content: String
)
```

`content` is the event's raw stored JSON — deserialize it with the concrete
event class matching `context.eventType` to get a typed event object.

### Redacting events

`redact` permanently rewrites a single event's content, identified by its
sequence number. `redactForEventSource` does the same for every event of a
given event source, optionally narrowed to specific event types (an empty
list, the default, redacts every event type for that source). **Both are
destructive, irreversible content rewrites — not a soft delete or a field
mask.** Once either call returns, the original content is gone from the
event store for good. Use them only for a confirmed compliance/erasure
request (e.g. GDPR "right to be forgotten"), typically alongside
[`IComplianceService.deleteEncryptionKey`](#icomplianceservice) rather than
instead of it — deleting the encryption key is non-destructive to event
content (it just becomes unreadable), while redaction actually erases it.

<!-- validate: skip -->

```kotlin
@JvmInline
value class RedactionReason(val value: String)

sealed class CompleteStreamResult {
    data class Success(
        val sequenceNumber: EventSequenceNumber
    ) : CompleteStreamResult()
    data object DefaultStreamCannotBeCompleted : CompleteStreamResult()
    data object AlreadyCompleted : CompleteStreamResult()
}
```

### Completing a stream

`completeStream` marks an event stream type/id pair as closed so no further
events can be appended to it, returning a `CompleteStreamResult`. The
default stream (`"Default"` paired with the default event stream id, the
one every plain `append`/`appendMany` call writes to) can never be
completed. Completing an already-completed stream returns
`CompleteStreamResult.AlreadyCompleted` rather than throwing.

### Observing appends: `appendOperations`

`appendOperations` is a hot `SharedFlow` that emits after every append made
*through that specific `IEventSequence` instance* completes, whether it
succeeded or failed — a single-event `append` emits a list of one element,
a batch `appendMany` emits the whole batch. It does not emit for
transactional appends made through `ITransactionalEventSequence` (those
only emit, as part of the underlying batch, once the unit of work commits
and the real append happens). Being a *hot* flow, only appends made after a
subscriber starts collecting are seen — nothing is replayed.

<!-- validate: skip -->

```kotlin
data class AppendedEventWithResult(
    val context: EventContext,
    val event: Any,
    val result: AppendResult
)
```

### Concurrency control

Optimistic concurrency is opt-in per append, via
`AppendOptions.concurrencyScope`:

<!-- validate: skip -->

```kotlin
data class AppendOptions(
    val correlationId: UUID? = null,
    val concurrencyScope: ConcurrencyScope? = null,
    val eventSourceType: String? = null,
    val eventStreamType: String? = null,
    val eventStreamId: String? = null,
    val subject: String? = null,
    val tags: List<String> = emptyList(),
    val occurred: Instant? = null,
    val causation: List<Causation> = emptyList()
)

data class ConcurrencyScope(
    val sequenceNumber: EventSequenceNumber,
    val eventSourceId: Boolean = false,
    val eventStreamType: String? = null,
    val eventStreamId: String? = null,
    val eventSourceType: String? = null,
    val eventTypes: List<EventTypeDescriptor> = emptyList()
)
```

Build one with `ConcurrencyScopeBuilder` rather than the constructor
directly — `withSequenceNumber` sets the expected position, and
`withEventSourceId`/`withEventStreamType`/`withEventStreamId`/
`withEventSourceType`/`withEventType(s)` each narrow which dimension the
check applies to. Leaving `concurrencyScope` unset (the default) is
equivalent to `ConcurrencyScope.none` — the append is not concurrency
checked. When the scope no longer matches, the append fails with
`AppendResult.concurrencyViolation` set instead of throwing.

### Causation

Every append carries a causation chain describing what led to it. By default
that chain is ambient: `CausationManager` builds one up per thread, and the
append reads it as it goes. Nearly every append should leave it at that.

Set `AppendOptions.causation` to attribute an append to a different chain —
an event imported from a legacy system, or a side effect that belongs to a
chain of its own rather than to the work the current thread happens to be
doing. An override deliberately leaves the ambient chain untouched, so later
appends are not attributed to something they had nothing to do with.

<!-- validate: body needs=store -->

```kotlin
import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.eventSequences.AppendOptions
import java.time.Instant

store.eventLog.append(
    "employee-1",
    EmployeeHired("Ada", "Lovelace", "Engineer"),
    AppendOptions(
        causation = listOf(
            Causation(
                Instant.parse("1998-06-01T09:00:00Z"),
                CausationType("LegacyImport"),
                mapOf("file" to "1998.csv")
            )
        )
    )
)
```

`EventForEventSourceId` carries the same field, which is how a reactor side
effect names its own chain. One caveat applies to batches: the kernel carries
a single chain per `appendMany`, not one per event, so a batch whose events
declare different chains throws `CausationDiffersAcrossBatch` rather than
silently keeping one of them. Give them the same causation, leave it unset,
or append them as separate batches. For a composed operation, set it once for
the whole batch with `withCausation`.

From Java, use `AppendOptionsBuilder.causation(...)`.

### AppendResult

| Property | Type | Description |
| --- | --- | --- |
| `isSuccess` | `Boolean` | `true` when there are no violations or errors |
| `sequenceNumber` | `EventSequenceNumber` | Position in the event log |
| `constraintViolations` | `List<ConstraintViolation>` | On failure |
| `errors` | `List<AppendError>` | On failure |
| `concurrencyViolation` | `ConcurrencyViolation?` | Set on stale scope |

---

## Unit of work

`IEventStore.unitOfWorkManager` returns an `IUnitOfWorkManager`, which
creates and tracks `IUnitOfWork` instances scoped to the current thread.

<!-- validate: skip -->

```kotlin
interface IUnitOfWorkManager {
    val current: IUnitOfWork
    val hasCurrent: Boolean
    fun tryGetFor(correlationId: CorrelationId): IUnitOfWork?
    fun begin(): IUnitOfWork
    fun begin(correlationId: CorrelationId): IUnitOfWork
    fun setCurrent(unitOfWork: IUnitOfWork)
}

interface IUnitOfWork {
    val isCompleted: Boolean
    val correlationId: CorrelationId
    val isSuccess: Boolean

    fun addEvent(
        eventSequenceId: EventSequenceId,
        eventSourceId: String,
        event: Any,
        options: AppendOptions? = null
    )
    fun getEvents(): List<Any>
    fun getConstraintViolations(): List<ConstraintViolation>
    fun getConcurrencyViolations(): List<ConcurrencyViolation>
    fun getAppendErrors(): List<AppendError>

    suspend fun commit()
    suspend fun rollback()

    fun onCompleted(callback: (IUnitOfWork) -> Unit)
    fun tryGetLastCommittedEventSequenceNumber(): EventSequenceNumber?
}
```

`current` throws `NoUnitOfWorkHasBeenStarted` if `begin` hasn't been called
on this thread — check `hasCurrent` first if that's expected. Prefer
staging events through `store.eventLog.transactional.append`/`appendMany`
(see [Transactions](../guides/transactions.md)) over calling `addEvent`
directly; the transactional event sequence resolves the right
`EventSequenceId` for you.

After `commit()`, `isSuccess` reflects whether every staged event was
appended without a constraint violation, concurrency violation, or append
error; `getConstraintViolations()`/`getConcurrencyViolations()`/
`getAppendErrors()` report the specifics, and
`tryGetLastCommittedEventSequenceNumber()` returns the highest sequence
number actually committed (`null` if nothing committed). `onCompleted`
registers a callback invoked once, whether the unit of work ends via
`commit()` or `rollback()` — it can be called multiple times to register
several callbacks.

---

## IIdentityManagerService

<!-- validate: skip -->

```kotlin
interface IIdentityManagerService {
    suspend fun rename(subject: String, name: String)
}
```

Renames the human-readable name the kernel has stored for the identity
identified by `subject`. This only updates the stored display name; it
does not change the `Identity` objects your process is currently using —
see
[Correlation and identity](/chronicle/concepts/correlation-identity-causation/).

---

## INamespacesService

<!-- validate: skip -->

```kotlin
interface INamespacesService {
    suspend fun ensure(namespaceName: String)
    suspend fun getAll(): List<String>
}
```

`ensure` creates a namespace if it doesn't already exist (idempotent).
`getAll` lists every namespace in the event store.

---

## IReactorsService

<!-- validate: skip -->

```kotlin
interface IReactorsService {
    suspend fun register(reactor: Any): Job
}
```

A handler takes the event first, and after that anything the client can supply
for it — an `EventContext`, a read model resolved for the event's event source,
or whatever an `IReactorMethodArgumentResolver` claims. Handlers may suspend.
`IReactorMiddleware` wraps every invocation. See
[Artifact Registration](../guides/artifact-registration.md) for both.

### Being told about a replay

Implement `ICanBeNotifiedAboutReplay` and declare `replayBegan` and/or
`replayEnded`. Each takes a `ReplayContext` or nothing at all, and may suspend.
Chronicle replays each event source independently, so the notifications arrive
once per partition rather than once overall.

<!-- validate: skip -->

```kotlin
data class ReplayContext(
    val observerId: String,
    val partition: String,
    val sequenceNumber: EventSequenceNumber
)
```

The kernel flags the first and last event of a replay rather than sending a
separate signal, so `replayBegan` runs immediately before the first replayed
event is handled and `replayEnded` immediately after the last. A replay that
delivers no events produces no notification — there was no first event to flag.

Implementing the interface and declaring neither method is rejected at
registration: a marker with nothing behind it would silently do nothing.

---

## IFailedPartitions

A handler that throws stops the event source it threw on and leaves every other
one running. That keeps one bad event from halting the system, and it also means
a stuck partition announces itself nowhere. `store.failedPartitions` is how an
application finds out, and how an operator recovers once the cause is fixed.

<!-- validate: skip -->

```kotlin
interface IFailedPartitions {
    suspend fun getFor(observerId: String): List<FailedPartition>
    suspend fun getFor(observerClass: KClass<*>): List<FailedPartition>
    suspend fun retry(
        observerId: String,
        partition: String,
        eventSequenceId: EventSequenceId = EventSequenceId.eventLog
    )
    suspend fun retry(observerClass: KClass<*>, partition: String)
}

data class FailedPartition(
    val id: UUID,
    val observerId: String,
    val partition: String,
    val attempts: List<FailedPartitionAttempt>
) {
    val lastAttempt: FailedPartitionAttempt?
}

data class FailedPartitionAttempt(
    val occurred: Instant,
    val sequenceNumber: EventSequenceNumber,
    val messages: List<String>,
    val stackTrace: String
)
```

The overloads taking a `KClass` read the observer's id — and, when retrying, the
sequence it observes — off the class exactly as registration does, so an observer
can be asked about by type rather than by remembering what its id came out as.

`attempts` is the history of the problem, oldest first: `lastAttempt` is the one
still standing in the way. Retrying an observer whose cause has not been fixed
simply adds another attempt, so fix first and retry after.

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

    suspend fun <T : Any> getInstances(
        readModelClass: KClass<T>,
        eventCount: Long? = null
    ): List<T>

    suspend fun <T : Any> getSnapshotsById(
        readModelClass: KClass<T>,
        key: String
    ): List<ReadModelSnapshot<T>>

    fun <T : Any> watch(readModelClass: KClass<T>): Flow<ReadModelChangeset<T>>

    suspend fun dehydrateSession(
        readModelClass: KClass<*>,
        key: String,
        sessionId: String
    )

    suspend fun <T : Any> release(instance: T, subject: String? = null): T
    suspend fun <T : Any> releaseMany(instances: List<T>): List<T>

    val materialized: IMaterializedReadModels
}
```

- `getInstances` replays events in-process to produce every instance of a
  read model, optionally capped to the first `eventCount` events.
- `getSnapshotsById` returns the full history of intermediate states for
  one read model key, grouped by correlation id — each `ReadModelSnapshot`
  carries the deserialized `instance`, the `events` that produced it, and
  (when known) when they `occurred` and their `correlationId`. Unlike
  `getInstanceByKey`, which only returns the latest state, this is the way
  to inspect how a read model got to where it is.
- `watch` returns a `Flow<ReadModelChangeset<T>>` of live changes,
  deserialized straight into `T` — no polling, no manual JSON parsing. Each
  `ReadModelChangeset` carries the `modelKey`, the `changeType`
  (`Added`/`Modified`/`Removed`), the deserialized `readModel` (`null` when
  `removed`), and the triggering event's `eventSequenceNumber`/`occurred`/
  `correlationId` when known.
- `release`/`releaseMany` decrypt `@Pii`-annotated properties on one or a
  batch of read model instances. The compliance subject defaults to each
  instance's `id` property if present; pass `subject` explicitly to
  `release` when there is no `id` property or it isn't the compliance
  subject.

---

## IReadModelReactors

<!-- validate: skip -->

```kotlin
interface IReadModelReactors {
    fun register(reactor: IReadModelReactor): Job
    fun stop()
}
```

Construct the implementation with the read models to watch through and the
event log any side-effect events are appended to:
`ReadModelReactors(store.readModels, store.eventLog)`. It is not reached
through `IEventStore`.

- `IReadModelReactor` is a marker interface. Dispatch is by convention: a
  method named `added`, `modified` or `removed` (matched
  case-insensitively) handles the corresponding `ReadModelChangeType`. Its
  first parameter is the read model — a single instance or a `List` of them
  — and that type selects which read model is watched. An optional second
  parameter of type `ReadModelChangeset` carries the change metadata.
- A removal never carries an instance, so a Kotlin `removed` handler must
  declare its read model parameter nullable. Handlers that cannot be
  dispatched to are rejected at registration with
  `InvalidHandlerSignature`.
- A handler may return an event, an `EventForEventSourceId`, or a `List` of
  either, to be appended as a side effect — bare events use the changed
  instance's key as the event source id. Return values that are not event
  types are ignored.
- `register` is not a suspending function and returns the `Job` backing that
  reactor's subscriptions; `stop` cancels every reactor registered through
  the instance.

See the [read model reactors guide](../guides/read-model-reactors.md) for
worked Kotlin and Java examples.

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

`IConstraintBuilder`, passed into `IConstraint.define`, can additionally
scope every constraint added through it — see
[Constraint scoping](../guides/constraints.md).

---

## IEventTypesService

<!-- validate: skip -->

```kotlin
interface IEventTypesService {
    suspend fun register(vararg eventClasses: KClass<*>)
    suspend fun registerSingle(eventClass: KClass<*>)
    suspend fun getAllGenerationsForEventType(
        eventTypeId: String
    ): List<Events.EventTypeRegistration>
    fun getRegisteredEventTypes(): List<EventTypeDescriptor>
}
```

`register`/`registerSingle` accept both plain `@EventType`-annotated
classes and [`IEventTypeMigration`](../guides/event-evolution.md) classes,
merging them into one registration per event type id, schema included
(generated from the latest generation's class — see
[Annotations](annotations.md)). `getRegisteredEventTypes` returns every
event type registered through this instance so far; it's what
`eventStoreSubscriptions.subscribe` falls back to when a subscription
isn't narrowed to specific event types.

---

## IEventSeedingService

<!-- validate: skip -->

```kotlin
interface IEventSeedingService {
    suspend fun seed(vararg seeders: Any)
}
```

See [Seeding](../guides/seeding.md) for `IEventSeedingBuilder`, including
namespace-scoped seed data via `forNamespace`.

---

## IComplianceService

<!-- validate: skip -->

```kotlin
interface IComplianceService {
    suspend fun release(
        subject: String,
        schema: String,
        payload: String
    ): String
    suspend fun deleteEncryptionKey(identifier: String)
}
```

`deleteEncryptionKey` deletes the encryption key for a compliance subject —
a "right to be forgotten" erasure that leaves existing encrypted PII
content permanently undecryptable, without rewriting the events
themselves. Compare with [`redact`/`redactForEventSource`](#redacting-events),
which instead rewrites event content directly.

---

## Java interop

Every service method above is a Kotlin `suspend` function and cannot be
called from Java directly. The `io.cratis.chronicle.java` package provides
blocking bridges — each is a static method taking the service (or
sequence) as its first argument. A few Kotlin value classes
(`EventSequenceNumber`, `EventSequenceId`, `EventTypeId`, `RedactionReason`)
have a private constructor on the JVM ABI and no ordinary getter, so
bridges that touch them take and return plain `long`/`String` instead.

- `EventLogJavaBridge` — `append`, `appendMany`, `hasEventsFor`,
  `getForEventSourceIdAndEventTypes`, `getFromSequenceNumber`,
  `getTailSequenceNumber`, `getNextSequenceNumber`, `completeStream`,
  `redact`, `redactForEventSource`, `watchAppendOperations`
- `TransactionalEventSequenceJavaBridge` — `append`, `appendMany`
- `ConcurrencyScopeBuilderJavaBridge` — `withSequenceNumber`
- `EventTypesServiceJavaBridge` — `register`, `registerSingle`,
  `getAllGenerationsForEventType`
- `ReadModelsJavaBridge` — `register`, `getInstanceByKey`,
  `getInstances`, `getSnapshotsById`, `watch`, `dehydrateSession`,
  `release`, `releaseMany`, `getMaterializedInstances`
- `ReactorsServiceJavaBridge` — `register`
- `ReducersServiceJavaBridge` — `register`
- `ProjectionsServiceJavaBridge` — `register`
- `ConstraintsServiceJavaBridge` — `register`
- `EventSeedingServiceJavaBridge` — `seed`
- `EventSeedingBuilderJavaBridge` / `EventSeedingScopeBuilderJavaBridge`
  — `forEventType`
- `NamespacesServiceJavaBridge` — `ensure`, `getAll`
- `IdentityManagerServiceJavaBridge` — `rename`
- `UnitOfWorkJavaBridge` — `commit`, `rollback`
- `ComplianceServiceJavaBridge` — `release`, `deleteEncryptionKey`

`ReadModelReactors` needs no bridge — its constructor takes plain types and
neither `register` nor `stop` is a suspending function, so Java calls both
directly.

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
`EventLogJavaBridge.getSequenceNumber(result)`. The same applies to
`getTailSequenceNumber`/`getNextSequenceNumber` (they return `long`
directly) and to `redact`/`redactForEventSource` (they take a `long`
sequence number and a plain `String` reason rather than
`EventSequenceNumber`/`RedactionReason`).

`ConstraintBuilderJavaBridge`, `UniqueConstraintBuilderJavaBridge`,
`ProjectionBuilderJavaBridge`, and `CausationManagerJavaBridge` cover the
builder APIs that take a `KClass` in Kotlin, accepting a `Class` instead.
