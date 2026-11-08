---
title: Artifact Registration
description: Learn how the JVM client discovers event types, read models, observers and constraints, registers them in order, and how to narrow, replace or disable it.
---

Chronicle's kernel has to know what your application is made of before it can do
anything useful with it. Event types need schemas registered so events can be
validated and migrated. Reducers and projections need to be declared so the
kernel knows what to feed them. Constraints need to exist before the append they
are meant to stop.

With automatic registration enabled (the default), the client discovers
classes carrying `@EventType`, `@EventSource`, `@ReadModel`, `@Reactor`,
`@Reducer`, `@Constraint` or `@FromEvent`, and implementations of
`IProjectionFor`, `ICanSeedEvents` or `IWebhookDefiner`. It registers them when
the client connects, in the order described below.

## The short version

Start with the four-field `EmployeeHired` declaration in
[Get started](../get-started/index.md#4-define-an-event-type) and a configured
client with automatic registration enabled. Keep these helpers in the same
package as that declaration, or import it from your application's package.
The event class must be discoverable; a helper method does not register it.
These are additions to an existing application, not standalone programs.

`MyApp` below is the event store, in the default namespace `Default`.
`employee-1` identifies the employee's event source; this event also carries
that id in its payload. Kotlin calls the suspending API from a coroutine:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.ChronicleClient
import io.cratis.chronicle.eventSequences.AppendResult

suspend fun appendRegisteredEmployee(client: ChronicleClient): AppendResult {
    val store = client.getEventStore("MyApp")
    store.awaitRegistration()
    val result = store.eventLog.append(
        "employee-1",
        EmployeeHired("employee-1", "Ada", "Lovelace", "Engineer")
    )
    if (result.isSuccess) {
        println("Accepted employee hire at ${result.sequenceNumber.value}")
    } else {
        System.err.println(
            "Employee hire was not confirmed: constraints=" +
                "${result.constraintViolations}, " +
                "errors=${result.errors}, concurrency=${result.concurrencyViolation}"
        )
    }
    return result
}
```

Java uses the blocking client. Call it from a thread that may block, not from
a Kotlin coroutine:

<!-- validate: declarations -->

```java
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.java.BlockingChronicleClient;

class RegisteredEmployeeAppender {
    static AppendResult appendRegisteredEmployee(
            BlockingChronicleClient client) {
        var store = client.getEventStore("MyApp");
        store.awaitRegistration();
        var result = store.getEventLog().append("employee-1",
            new EmployeeHired("employee-1", "Ada", "Lovelace", "Engineer"));
        if (result.isSuccess()) {
            System.out.println("Accepted employee hire at " +
                result.getSequenceNumberValue());
        } else {
            System.err.println("Employee hire was not confirmed: constraints=" +
                result.getConstraintViolations() + ", errors=" +
                result.getErrors() +
                ", concurrency=" + result.getConcurrencyViolation());
        }
        return result;
    }
}
```

`append` returns an `AppendResult`. Use the accepted position only when
`isSuccess` (Java: `isSuccess()`) is true. Otherwise inspect
`constraintViolations`, `errors` and `concurrencyViolation`; Java exposes them
through the getters above. The helper returns the result so the caller can
surface the problem or apply its recovery policy. A connection or command
failure can throw instead of returning a result. Do not automatically append
again after an error or a lost response: an unconfirmed outcome is not proof
that nothing was stored.

`awaitRegistration()` waits for the first registration **attempt**, not its
success. It is optional: the first append waits for the same attempt. Calling
it at startup moves that wait out of your first request. It has no time limit
of its own; apply the
[bounded startup wait](../reference/connection-lifecycle.md#bound-the-first-call)
before accepting requests. Check registration diagnostics as well as the
append result.

A successful append confirms storage, not that an observer or read model has
caught up. If the next action reads derived state, see
[Reading after writing](../reference/connection-lifecycle.md#reading-after-writing).
Keep the configured client for the application's lifetime and
[dispose it when the application stops](../reference/connection-lifecycle.md#disposing).
Named source and stream routing is optional; see
[Event sources and streams](event-sources.md) when you need it.

In a Spring Boot application the starter already waits for it at startup, for
up to `cratis.chronicle.registration-timeout`. See [Spring Boot](spring-boot.md#startup).

## What gets discovered

| Artifact | Recognized by |
| --- | --- |
| Event type | `@EventType` |
| Event type migration | Implements `IEventTypeMigration` |
| Event source definition | `@EventSource` |
| Read model | `@ReadModel` |
| Projection | Implements `IProjectionFor` |
| Model-bound projection | Read model carrying `@FromEvent` |
| Reactor | `@Reactor` |
| Reducer | `@Reducer` |
| Constraint | Implements `IConstraint` |
| Event seeder | Implements `ICanSeedEvents` |
| Webhook | Implements `IWebhookDefiner` |
| Capture | Implements `ICapture` |
| Reactor middleware | Implements `IReactorMiddleware` |
| Reactor argument resolver | Implements `IReactorMethodArgumentResolver` |

Only concrete classes qualify — interfaces and abstract classes are skipped, so
your own base types never get registered by accident.

The two reactor entries are client-side: they are never declared to the kernel,
they take part in how a reactor handler is invoked. They are discovered here
because they answer the same question — what is this application made of? See
[Reactor middlewares](#reactor-middlewares) below.

External services and event store subscriptions are configuration rather than
artifacts: they describe systems outside your application, so they are still set
up explicitly through `store.externalServices` and `store.eventStoreSubscriptions`.

## The order things are registered in

The automatic pass uses this order:

1. **Event types and their migrations**, together in one call, because
   everything else is expressed in terms of event type ids and because the
   kernel merges an event type's generations from a single registration.
2. **Event source definitions.** Routing is independent of event types; this
   stage registers the discovered definitions without attaching them to events.
3. **Read models that no observer produces.** A read model built by a reducer or
   a projection is registered by that observer instead, which is the only place
   the observer's type and identity are known.
4. **Constraints, projections and webhooks.**
5. **Reactors and reducers**, which start observing.
6. **Captures**, which start appending the moment they run, so like seeding they
   go behind every observer that should see what they bring in.
7. **Seeders**, last of all — the kernel appends seeded events immediately, so
   every observer that should see them has to be watching first.

Registration runs again on every reconnect, because a kernel that restarted has
forgotten what it was told. Reactors and reducers are started only once: each one
re-establishes its own observation when the connection comes back.

## When registration fails

Reactors and reducers are started one at a time. A failure to start one is
collected while the client tries the others, then captures and seeders. If
those later steps finish, the pass throws `ArtifactRegistrationFailed` with
the collected failures. During automatic registration, the event store writes
a line for each failed observer to standard error, in this form:

```text
[EventStore] Automatic registration of 'com.acme.Notifications' failed: <message>
```

The failed observer is tried again on the next registration pass, after a
reconnect or when you call `registerAll()`; one that started is never started
twice. Cancellation stops the pass rather than becoming an observer failure.

Earlier steps (event types, event source definitions, read models, constraints,
projections and webhooks) stop the pass when they throw. A capture or seeder
failure also stops the pass, before collected observer failures can be
reported. Automatic registration writes such a failure to standard error in
this form:

```text
[EventStore] Automatic registration of artifacts failed: <message>
```

The pass runs again on the next reconnect.

The registration wait is released even when the pass fails. A normal return
from `awaitRegistration()` does not prove registration succeeded; an append
can still fail because its event type was not registered. Check standard error
after startup and inspect the append result. A rejected connection is a
separate failure and can make the waiting calls throw; see
[Connection lifecycle](../reference/connection-lifecycle.md#rejected-connections).

Common causes:

- A reactor or reducer with no handler method throws `ObserverHasNoHandlers`.
  A handler is a public method whose first parameter is an `@EventType` class.
- An artifact the default activator cannot create throws
  `ArtifactActivationFailed`; see [Artifacts with dependencies](#artifacts-with-dependencies).
- A reactor handler asks for a parameter nothing can supply.

An `IConstraint` needs `@Constraint` as well. Without the annotation it is
skipped without a message.

## Narrowing the scan

To exclude third-party artifacts or limit discovery to part of an application,
point the scan at the packages you own:

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
var options = ChronicleOptions.development().withoutAutoRegistration();
var client = BlockingChronicleClient.connect(options);
var store = client.getEventStore("MyApp");

// Event types are not wrapped by the blocking store; use the static bridge.
EventTypesServiceJavaBridge.register(store.unwrap().getEventTypes(),
    EmployeeHired.class, EmployeePromoted.class);
store.getReducers().register(new EmployeeStateReducer());
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

Spring Boot applications get this for free — see [Spring Boot](spring-boot.md).

If an artifact cannot be created, the client throws `ArtifactActivationFailed`
naming the class and explaining the three ways out: give it a constructor that
takes nothing, give its parameters defaults, or activate it through a container.

## Reactor middlewares

Tracing, logging, metrics and correlation scoping want to happen around every
reactor handler and belong to none of them. Put them in an `IReactorMiddleware`
and every reactor stays a description of what happens when a fact arrives:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.IReactorMiddleware

class HandlerLogging : IReactorMiddleware {
    override suspend fun beforeInvoke(context: EventContext, event: Any) {
        val name = event::class.simpleName
        println("Handling $name #${context.sequenceNumber} for ${context.eventSourceId}")
    }

    override suspend fun afterInvoke(context: EventContext, event: Any) {
        println("Handled ${event::class.simpleName} #${context.sequenceNumber}")
    }
}
```

Writing the class is all there is to it — discovery finds it and applies it to
every reactor. `beforeInvoke` runs outermost-first and `afterInvoke` in reverse,
so middlewares nest the way you would write them by hand, and `afterInvoke` runs
whether the handler returned or threw.

One middleware instance serves every reactor, and a suspending handler can
resume on a different thread, so `afterInvoke` may run on another thread than
`beforeInvoke`. Keep per-invocation state out of fields and `ThreadLocal`s.

Java cannot implement a suspending method, so a Java middleware implements
`BlockingReactorMiddleware` — the same two methods without `suspend` — and the
client adapts it onto the same chain.

## Handler parameters beyond the event

A reactor handler takes the event, and optionally its `EventContext`. Anything
past that is resolved per invocation, which is how a handler asks for a read
model without reaching for the event log:

<!-- validate: skip -->

```kotlin
@Reactor
class OverdraftAlerts(private val mail: Mailer) {
    suspend fun moneyWithdrawn(
        event: MoneyWithdrawn,
        account: AccountBalance?
    ) {
        if ((account?.balance ?: 0.0) < 0.0) mail.overdrawn(event.accountId)
    }
}
```

The instance is fetched for the event source the event arrived under, and is
`null` when nothing has been projected for that key yet, so declare the
parameter nullable. It is read from the read model store, which is updated
asynchronously: it may not include the event being handled, or events just
before it. Do not base a decision that must be exact on it; enforce such rules
with a [constraint](/chronicle/constraints/) when the event is appended.

Implement `IReactorMethodArgumentResolver` to supply anything else, a
container-backed service for instance. Discovered resolvers are consulted before
the built-in read model one, so an application can take over a parameter the
client would otherwise claim. A parameter nothing can supply is rejected when the
reactor registers, rather than failing on every event.

## Reference

| Option | Default | Description |
| --- | --- | --- |
| `autoDiscoverAndRegister` | `true` | Register artifacts on connect |
| `artifacts` | `ClientArtifacts.default` | What the application is made of |
| `artifactActivator` | `ArtifactActivator` | How artifacts are created |

| Method on `IEventStore` | Description |
| --- | --- |
| `registerAll()` | Register everything now. Safe to call repeatedly |
| `awaitRegistration()` | Wait for the first registration attempt |

This wait does not prove registration succeeded. It returns at once when
automatic registration is off.
