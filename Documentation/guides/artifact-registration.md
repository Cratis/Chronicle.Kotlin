# Artifact Registration

Chronicle's kernel has to know what your application is made of before it can do
anything useful with it. Event types need schemas registered so events can be
validated and migrated. Reducers and projections need to be declared so the
kernel knows what to feed them. Constraints need to exist before the append they
are meant to stop.

The client does all of that for you. Every class carrying `@EventType`,
`@ReadModel`, `@Reactor`, `@Reducer`, `@Constraint`, `@FromEvent`, or
implementing `IProjectionFor`, `ICanSeedEvents` or `IWebhookDefiner` is found on
the classpath and registered the moment the client connects — in the order the
kernel needs them.

## The short version

Write your artifacts. Connect. That is the whole setup:

<!-- validate: skip -->

```kotlin
val client = ChronicleClient(ChronicleOptions.development())
val store = client.getEventStore("MyApp")
store.awaitRegistration()

store.eventLog.append("employee-1", EmployeeHired("Ada", "Lovelace", "Engineer"))
```

From Java:

<!-- validate: skip -->

```java
var client = new ChronicleClient(ChronicleOptions.Companion.development());
EventStore store = client.getEventStore("MyApp", "Default");
EventStoreJavaBridge.awaitRegistration(store);

EventLogJavaBridge.append(store.getEventLog(), "employee-1",
    new EmployeeHired("Ada", "Lovelace", "Engineer"), null);
```

`awaitRegistration()` returns as soon as the first registration pass is through.
It is not required — appending before it completes simply races the kernel — but
calling it once at startup makes the first append deterministic.

## What gets discovered

| Artifact | Recognized by |
| --- | --- |
| Event type | `@EventType` |
| Event type migration | Implements `IEventTypeMigration` |
| Read model | `@ReadModel` |
| Projection | Implements `IProjectionFor` |
| Model-bound projection | Read model carrying `@FromEvent` |
| Reactor | `@Reactor` |
| Reducer | `@Reducer` |
| Constraint | Implements `IConstraint` |
| Event seeder | Implements `ICanSeedEvents` |
| Webhook | Implements `IWebhookDefiner` |

Only concrete classes qualify — interfaces and abstract classes are skipped, so
your own base types never get registered by accident.

External services and event store subscriptions are configuration rather than
artifacts: they describe systems outside your application, so they are still set
up explicitly through `store.externalServices` and `store.eventStoreSubscriptions`.

## The order things are registered in

Order matters, and the client gets it right so you do not have to think about it:

1. **Event types and their migrations**, together in one call, because
   everything else is expressed in terms of event type ids and because the
   kernel merges an event type's generations from a single registration.
2. **Read models that no observer produces.** A read model built by a reducer or
   a projection is registered by that observer instead, which is the only place
   the observer's type and identity are known.
3. **Constraints, projections and webhooks.**
4. **Reactors and reducers**, which start observing.
5. **Seeders**, last of all — the kernel appends seeded events immediately, so
   every observer that should see them has to be watching first.

Registration runs again on every reconnect, because a kernel that restarted has
forgotten what it was told. Reactors and reducers are started only once: each one
re-establishes its own observation when the connection comes back.

## Narrowing the scan

Scanning the whole classpath is fine for most applications and takes a fraction
of a second. In a large one, or where third-party libraries ship Chronicle
artifacts of their own, point discovery at the packages you own:

<!-- validate: skip -->

```kotlin
val options = ChronicleOptions.development()
    .withArtifactsFrom("com.acme.ordering", "com.acme.shipping")
```

Sub-packages are included, so one entry per top-level package is usually enough.

## Listing artifacts explicitly

`KnownClientArtifacts` takes the list instead of finding it — useful where
classpath scanning is unwanted, such as a locked-down runtime or a spec that must
not see the rest of the classpath. Each class is sorted into every kind it
qualifies for, so a `@ReadModel` carrying `@FromEvent` only has to be listed once:

<!-- validate: skip -->

```kotlin
import io.cratis.chronicle.artifacts.KnownClientArtifacts

val options = ChronicleOptions.development().copy(
    artifacts = KnownClientArtifacts(
        EmployeeHired::class,
        EmployeeState::class,
        EmployeeStateReducer::class,
        UniqueEmployeeEmail::class
    )
)
```

## Turning it off

Manual registration is still fully supported. Turn discovery off and register
whatever you like, whenever you like:

<!-- validate: skip -->

```kotlin
val client = ChronicleClient(ChronicleOptions.development().withoutAutoRegistration())
val store = client.getEventStore("MyApp")

store.eventTypes.register(EmployeeHired::class, EmployeePromoted::class)
store.reducers.register(EmployeeStateReducer())
store.constraints.register(UniqueEmployeeEmail())
```

From Java:

<!-- validate: skip -->

```java
var options = ChronicleOptions.Companion.development().withoutAutoRegistration();
var client = new ChronicleClient(options);
EventStore store = client.getEventStore("MyApp", "Default");

EventTypesServiceJavaBridge.register(store.getEventTypes(),
    EmployeeHired.class, EmployeePromoted.class);
ReducersServiceJavaBridge.register(store.getReducers(), new EmployeeStateReducer());
```

With auto-registration off, `awaitRegistration()` returns immediately — there is
nothing to wait for. `store.registerAll()` still works, and registers everything
discovery found in one call, so you can keep discovery and merely control *when*
it happens.

The console samples in `Samples/Kotlin/Console` and `Samples/Java/Console`
deliberately opt out, so they double as a tour of the registration API.

## Artifacts with dependencies

By default an artifact is created by calling a constructor that takes no
arguments — either a genuine no-arg constructor, or one where every parameter has
a default. That covers reactors, reducers, constraints and seeders as they are
normally written.

An artifact that needs collaborators should be created by a container. Supply an
`IArtifactActivator` and the client will ask it instead:

<!-- validate: skip -->

```kotlin
import io.cratis.chronicle.artifacts.IArtifactActivator

val options = ChronicleOptions.development().copy(
    artifactActivator = IArtifactActivator { type -> myContainer.resolve(type) }
)
```

If an artifact cannot be created, the client throws `ArtifactActivationFailed`
naming the class and explaining the three ways out: give it a constructor that
takes nothing, give its parameters defaults, or activate it through a container.

## Reference

| Option | Default | Description |
| --- | --- | --- |
| `autoDiscoverAndRegister` | `true` | Register artifacts on connect |
| `artifacts` | `ClientArtifacts.default` | What the application is made of |
| `artifactActivator` | `ArtifactActivator` | How artifacts are created |

| Method on `IEventStore` | Description |
| --- | --- |
| `registerAll()` | Register everything now. Safe to call repeatedly |
| `awaitRegistration()` | Wait for it. Returns at once when it is off |
