# Seeding

This page shows how to seed events using the Chronicle Kotlin client.
Seeding is sent to the Chronicle Server when the event store connects, and
the server applies it once per namespace. See [Event
Seeding](/chronicle/event-seeding/) for the concept this page assumes.

## Define events

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventType

@EventType(id = "account-opened")
data class AccountOpened(
    val accountId: String,
    val ownerName: String,
    val initialBalance: Double
)

@EventType(id = "funds-deposited")
data class FundsDeposited(val accountId: String, val amount: Double)
```

## Implement a seeder

Annotate a class with `@Seeder`, implement `ICanSeedEvents`, and use
`IEventSeedingBuilder.forEventSource` to accumulate events:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.seeding.ICanSeedEvents
import io.cratis.chronicle.seeding.IEventSeedingBuilder
import io.cratis.chronicle.seeding.Seeder

@Seeder
class AccountSeeder : ICanSeedEvents {
    override fun seed(builder: IEventSeedingBuilder) {
        builder.forEventSource(
            "account-1",
            listOf(AccountOpened("account-1", "Alice", 1000.0))
        )
    }
}
```

## Seed mixed event types for one event source

`forEventSource` takes a list of any event types, so mixing types for the
same event source is the same call:

<!-- validate: declarations -->

```kotlin
@Seeder
class MixedAccountSeeder : ICanSeedEvents {
    override fun seed(builder: IEventSeedingBuilder) {
        builder.forEventSource(
            "account-1",
            listOf(
                AccountOpened("account-1", "Alice", 1000.0),
                FundsDeposited("account-1", 500.0)
            )
        )
    }
}
```

Chain multiple calls to seed several event sources:

<!-- validate: declarations -->

```kotlin
@Seeder
class MultiAccountSeeder : ICanSeedEvents {
    override fun seed(builder: IEventSeedingBuilder) {
        builder
            .forEventSource(
                "account-1",
                listOf(AccountOpened("account-1", "Alice", 1000.0))
            )
            .forEventSource(
                "account-2",
                listOf(AccountOpened("account-2", "Bob", 500.0))
            )
    }
}
```

## Seed a specific event type

`forEventType` is an alternative to `forEventSource` that also checks the
event class is annotated with `@EventType` before accepting it — useful
when a seeder should only ever seed one specific event type and you want
that caught early rather than at the server:

<!-- validate: declarations -->

```kotlin
@Seeder
class TypedAccountSeeder : ICanSeedEvents {
    override fun seed(builder: IEventSeedingBuilder) {
        builder.forEventType(
            AccountOpened::class,
            "account-1",
            listOf(AccountOpened("account-1", "Alice", 1000.0))
        )
    }
}
```

## Seed a specific namespace

By default, seed data targets the event store's own namespace. Call
`forNamespace` to target a different one instead — it returns a scoped
builder whose `forEventSource`/`forEventType` calls all seed into that
namespace, leaving calls made directly on `builder` targeting the event
store's own namespace as before:

<!-- validate: declarations -->

```kotlin
@Seeder
class MultiNamespaceAccountSeeder : ICanSeedEvents {
    override fun seed(builder: IEventSeedingBuilder) {
        builder
            .forEventSource(
                "account-1",
                listOf(AccountOpened("account-1", "Alice", 1000.0))
            )
            .forNamespace("staging")
            .forEventSource(
                "account-1",
                listOf(AccountOpened("account-1", "Staging Alice", 250.0))
            )
    }
}
```

## Running seeders

Pass seeder instances to the event store's `seeding` service:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.IEventStore

suspend fun runSeeders(store: IEventStore) {
    store.seeding.seed(AccountSeeder())
}
```

## How it runs

- Seed batches are sent to the Chronicle Server when the event store connects.
- The server deduplicates seeded events and applies them once per namespace.
- Events are appended in a single batch for efficient startup.

## Best practices

- Keep seed data minimal and deterministic.
- Use clear event source IDs to make debugging easier.
- Group seeders by scenario so you can remove or adjust them easily.
- Only call `store.seeding.seed(...)` when you want seeding to run — for
  example, guard it behind a development-only build flag or configuration
  check.
