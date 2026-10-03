---
title: Event sources and streams
description: Define named event sources with streams and concurrency dimensions, register them, and append through them from Kotlin and Java.
---

An event source definition gives an event source a stable name, an optional
description, the streams it uses and the concurrency dimensions an append to
it should be checked on. Definitions are optional. Appending without one
behaves exactly as it always has.

Routing belongs to the append, not to the event type: any event can be
appended through any definition, and a definition is never attached to an
event type. This needs a Chronicle 19.30.0 or later kernel.

## Define an event source

Annotate a carrier class with `@EventSource`. The class is never
instantiated, so it can be empty. Without an explicit name, the class name
minus an `EventSource` suffix is used.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.eventSources.ConcurrencyDimension
import io.cratis.chronicle.eventSources.EventSource
import io.cratis.chronicle.eventSources.EventStream

@EventSource(
    name = "Account",
    description = "A customer account",
    concurrency = [ConcurrencyDimension.EventSourceId],
    streams = [
        EventStream(
            name = "Transactions",
            description = "Money in and out",
            concurrency = [
                ConcurrencyDimension.EventSourceId,
                ConcurrencyDimension.EventStreamType
            ]
        ),
        EventStream("Settings")
    ]
)
class AccountEventSource
```

<!-- validate: declarations -->

```java
import io.cratis.chronicle.eventSources.ConcurrencyDimension;
import io.cratis.chronicle.eventSources.EventSource;
import io.cratis.chronicle.eventSources.EventStream;

@EventSource(
    name = "Account",
    description = "A customer account",
    concurrency = ConcurrencyDimension.EventSourceId,
    streams = {
        @EventStream(
            name = "Transactions",
            description = "Money in and out",
            concurrency = {
                ConcurrencyDimension.EventSourceId,
                ConcurrencyDimension.EventStreamType
            }),
        @EventStream(name = "Settings")
    })
class AccountEventSource {
}
```

Source names must be unique, and a stream name must be unique within its
source. Duplicates and blank names fail with a clear exception when the
definitions are discovered, before anything is sent to the kernel. A stream
that declares no concurrency dimensions uses the ones of its event source.

## Register definitions

Definitions are found with the other artifacts and registered at startup,
right after event types, when `autoDiscoverAndRegister` is on. To register by
hand, call `eventStore.eventSources.register()`. Registration is an upsert:
the kernel keeps definitions the client no longer declares.

`eventSources` is a member of `EventStore`. Through the `IEventStore`
interface it is the extension `io.cratis.chronicle.eventSources`, which
returns the registry of an event store that implements
`IEventSourcesCapability` and an unsupported one otherwise. Declaring an
`@EventSource` class against a store without the capability fails
registration instead of silently dropping the definition.

## Append through a definition

`appendThroughEventSource` stamps the event source type from the definition,
checks the stream against the ones it declares, and records the event source
name on the event. `appendManyThroughEventSource` does the same for an atomic
batch.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.eventSequences.IEventSequence
import io.cratis.chronicle.eventSequences.appendThroughEventSource

suspend fun deposit(
    sequence: IEventSequence,
    accountId: String,
    deposited: Any
) = sequence.appendThroughEventSource(
    AccountEventSource::class,
    accountId,
    deposited,
    eventStream = "Transactions"
)
```

From Java, name the definition through the options:

<!-- validate: declarations -->

```java
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.BlockingEventSequence;

class Deposits {
    static AppendResult deposit(
            BlockingEventSequence sequence,
            String accountId,
            Object deposited) {
        var options = new AppendOptionsBuilder()
            .throughEventSource(AccountEventSource.class, "Transactions")
            .build();
        return sequence.append(accountId, deposited, options);
    }
}
```

These errors are raised before anything is sent:

- `UnknownEventSource`: the class is not a discovered definition.
- `EventStreamDoesNotBelongToEventSource`: the stream is not declared by
  the definition.
- `EventRoutingContradictsEventSource`: an explicit `eventSourceType` or
  `eventStreamType` disagrees with the definition.

A batch is validated as a whole, so one bad event fails the batch and
nothing is appended.

## Route each event of a batch

`EventForEventSourceId` takes an `eventSource` and an `eventStream`, so one
atomic batch can span several definitions. A per-event definition applies to
that event only, and events without one are appended as before.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.IEventSequence

suspend fun transfer(
    sequence: IEventSequence,
    from: String,
    to: String,
    withdrawn: Any,
    deposited: Any
) = sequence.appendMany(
    listOf(
        EventForEventSourceId(
            from,
            withdrawn,
            eventSource = AccountEventSource::class,
            eventStream = "Transactions"
        ),
        EventForEventSourceId(
            to,
            deposited,
            eventSource = AccountEventSource::class,
            eventStream = "Transactions"
        )
    )
)
```

## Concurrency

When you do not pass a `concurrencyScope`, the definition's dimensions
(the stream's, or the event source's when the stream has none) are turned
into a scope from the current tail of the matching events. A dimension is
scoped only when its value is known: the event source id and event source
type always are, a stream type or id only when one is in play. When the
definition declares no dimensions, the append stays unchecked, exactly like
an append without a definition.

An explicit `concurrencyScope` always wins, in single appends and for any
event source id in a batch's `concurrencyScopes` map.

In a batch the kernel takes one scope per event source id. The client derives
the guard for every event from its own definition and stream, never from the
first event that mentions the id. Events that derive no guard never suppress
one that does, in any order. Guarded events that select on the same event
source, stream type, stream id and source type share the first guard taken,
whatever the current tail is. If they select differently, for example one
through the `Transactions` stream and one through the event source alone, the
batch is rejected with `ConflictingEventSourceConcurrency` before anything is
appended, because applying either guard would guard the other event wrongly.
Pass an explicit scope for that id, or append the events in separate batches.

## Read the event source name

Events appended through a definition carry its name in
`EventContext.eventSource` — when read back and when delivered to reactors
and reducers. Events appended without one, including events stored before
event sources existed, have an empty `eventSource`; the client does not make
up a name.

## Compatibility

Event sources are an additive, optional feature. Code that does not use them
behaves as before, and they need a kernel at 19.30.0 or later.

- **Custom `IEventStore` and `IClientArtifacts` implementations** keep
  compiling and linking, in Kotlin and Java, compiled or not. Neither interface
  gained a member: the registry is the optional `IEventSourcesCapability`, and
  the declared classes are the optional `IEventSourceArtifacts`. Only
  `EventStore`, the Spring `ResolvedEventStore`, `ClientArtifacts` and
  `KnownClientArtifacts` implement them. Read them through the
  `IEventStore.eventSources` and `IClientArtifacts.eventSources` extensions: a
  store without the capability reports an unsupported registry, an artifacts
  implementation without it reports no event sources. Declaring `@EventSource`
  classes against a store that does not support them fails at registration.
- **Source and binary compatibility** is preserved for `EventContext`,
  `AppendOptions` and `EventForEventSourceId`. The constructors and `copy`
  methods from before event sources exist with their original defaults, so the
  synthetic default-argument constructors and `copy$default` methods that
  Kotlin code compiled against an earlier release calls are still generated.
  That code, and Java callers using the short positional forms, runs against
  this release without recompiling. `EventLog` and `EventSequence` keep their
  previous constructors.
- The in-memory test sequence cannot honor a definition; appending through one
  fails with `UnsupportedOperationException` instead of dropping the metadata.
