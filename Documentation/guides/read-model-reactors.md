# Read Model Reactors

`store.readModels.watch(...)` hands you a `Flow` of changesets — and with it
the job of collecting it, branching on the change type, keeping the
subscription alive, and doing it again for every read model you care about.

A read model reactor takes that plumbing away. You write one method per
change, register the class, and Chronicle dispatches to it. Where a
[reactor](./reactors.md) reacts to events, a read model reactor reacts to
the *state those events produced* — an instance being added, modified, or
removed.

## Writing a reactor

Implement `IReadModelReactor` and name a method after the change it handles:
`added`, `modified` or `removed`. The first parameter is the read model, and
its type is what decides which read model the reactor watches — the same way
a reactor's first parameter selects its event type.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.readModels.IReadModelReactor
import io.cratis.chronicle.readModels.ReadModelChangeset

class EmployeeProfileAlerts : IReadModelReactor {
    fun added(profile: EmployeeProfile) {
        println("${profile.firstName} joined ${profile.department}")
    }

    fun modified(
        profile: EmployeeProfile,
        changeset: ReadModelChangeset<EmployeeProfile>
    ) {
        println("${changeset.modelKey} changed at " +
                "sequence ${changeset.eventSequenceNumber}")
    }
}
```

The optional second parameter is the `ReadModelChangeset` that caused the
call. Take it when you need the instance's `modelKey`, the
`eventSequenceNumber` that produced the change, or its `correlationId`; leave
it off when the instance itself is all you need.

Method names are matched case-insensitively, so `Added` works as well as
`added`.

## Reacting to removals

A removal never carries an instance — there is nothing left to hand you — so
a Kotlin `removed` handler must declare its read model parameter nullable.
The key of the instance that went away is on the changeset:

<!-- validate: declarations -->

```kotlin
class EmployeeProfileOffboarding : IReadModelReactor {
    fun removed(
        profile: EmployeeProfile?,
        changeset: ReadModelChangeset<EmployeeProfile>
    ) {
        println("${changeset.modelKey} is gone")
    }
}
```

Declaring it non-nullable fails at registration with
`InvalidHandlerSignature` rather than throwing on every removal that
arrives.

## Watching more than one read model

Each handler's first parameter stands on its own, so one reactor can cover
several read models. Chronicle opens one watch per read model it finds:

<!-- validate: declarations -->

```kotlin
class HrDashboardAlerts : IReadModelReactor {
    fun added(profile: EmployeeProfile) {
        println("profile added: ${profile.id}")
    }

    fun added(overview: OrderOverview) {
        println("order added: ${overview.id}")
    }
}
```

A handler can also take a `List` of read models instead of a single
instance; the element type is then what selects the read model.

## Returning events as side effects

Like a reactor, a handler may return an event instead of taking a dependency
on the event log. It is appended for you, using the changed instance's key
as the event source id:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class EmployeeProfileArchived(val employeeId: String)

class EmployeeProfileArchiver : IReadModelReactor {
    fun removed(
        profile: EmployeeProfile?,
        changeset: ReadModelChangeset<EmployeeProfile>
    ) = EmployeeProfileArchived(changeset.modelKey)
}
```

Return a `List` to append several events, and wrap one in
`EventForEventSourceId` to send it to an event source other than the changed
instance's key. Anything that is not an event type — including `Unit` — is
ignored, so handlers that just do their work and return nothing are fine.

## Registering and stopping

`ReadModelReactors` needs the read models to watch through and the event log
to append side effects to. Registration is not a suspending call: it starts
the subscriptions and returns the `Job` backing them.

<!-- validate: body needs=store -->

```kotlin
import io.cratis.chronicle.readModels.ReadModelReactors

val readModelReactors = ReadModelReactors(store.readModels, store.eventLog)
readModelReactors.register(EmployeeProfileAlerts())
readModelReactors.register(EmployeeProfileOffboarding())

// Later, when shutting down:
readModelReactors.stop()
```

`stop()` cancels every reactor registered through that instance. Cancel the
returned `Job` instead when you want to stop a single reactor.

Handlers may be called more than once for the same change — a watch that
ends is re-established — so keep them idempotent, exactly as you would a
reactor.

## Java

`IReadModelReactor` is a plain marker interface and `register` is not a
suspending function, so Java uses both directly with no bridge. Java
parameters carry no nullability, so a Java `removed` handler declares the
read model type as it is:

<!-- validate: declarations -->

```java
import io.cratis.chronicle.readModels.IReadModelReactor;
import io.cratis.chronicle.readModels.ReadModelChangeset;

public class JavaEmployeeProfileAlerts implements IReadModelReactor {
    public void added(EmployeeProfile profile) {
        System.out.println(profile.getFirstName() + " joined");
    }

    public void removed(
            EmployeeProfile profile,
            ReadModelChangeset<EmployeeProfile> changeset) {
        System.out.println(changeset.getModelKey() + " is gone");
    }
}
```

<!-- validate: body needs=store -->

```java
import io.cratis.chronicle.readModels.ReadModelReactors;

ReadModelReactors readModelReactors =
        new ReadModelReactors(store.getReadModels(), store.getEventLog());
readModelReactors.register(new JavaEmployeeProfileAlerts());
```

See the [EventStore API reference](../reference/event-store-api.md) for the
full `IReadModelReactors` surface, and
[Read Models](../concepts/read-models.md) for the imperative `watch` this
builds on.
