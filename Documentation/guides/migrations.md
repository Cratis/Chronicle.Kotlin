# Event Type Migrations

This page shows how to migrate an event type between generations using the
Chronicle Kotlin client. A migration describes how to upcast an older
generation of an event to a newer one (and, optionally, downcast back), so
consumers written against either generation keep working after the shape of
an event changes. See [Event type migrations](/chronicle/concepts/event-type-migrations/)
and [Migrations](/chronicle/migrations/) for the concepts this page assumes.

## Defining two generations

Both generations share the same `id` but differ in `generation`. The target
generation must be exactly one more than the source:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventType

@EventType(id = "order-placed", generation = 1)
data class OrderPlacedV1(val orderId: String)

@EventType(id = "order-placed", generation = 2)
data class OrderPlacedV2(val orderId: String, val currency: String)
```

## Implementing a migration

Extend `EventTypeMigration<TTarget, TSource>`, passing the target and
source classes to the constructor, and override `upcast` and/or `downcast`
to describe the property transformations with an `EventTypeMigrationBuilder`:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.migrations.EventTypeMigration
import io.cratis.chronicle.events.migrations.EventTypeMigrationBuilder

class OrderPlacedMigration : EventTypeMigration<OrderPlacedV2, OrderPlacedV1>(
    OrderPlacedV2::class,
    OrderPlacedV1::class
) {
    override fun upcast(
        builder: EventTypeMigrationBuilder<OrderPlacedV2, OrderPlacedV1>
    ) {
        builder.defaultValue(OrderPlacedV2::currency, "USD")
    }
}
```

`upcast` and `downcast` both default to a no-op, so implement only the
direction(s) you need.

## Builder operations

`EventTypeMigrationBuilder` supports four operations, each keyed by the
target property being populated. Properties left unmapped are matched by
name automatically, so you only call the builder for properties that
genuinely changed shape.

Rename a property that moved from one name to another:

<!-- validate: declarations -->

```kotlin
@EventType(id = "customer-registered", generation = 2)
data class CustomerRegisteredV2(val customerId: String)

@EventType(id = "customer-registered", generation = 1)
data class CustomerRegisteredV1(val buyerId: String)
```

<!-- validate: skip -->

```kotlin
// Inside upcast(builder: EventTypeMigrationBuilder<CustomerRegisteredV2, CustomerRegisteredV1>):
builder.renamedFrom(CustomerRegisteredV2::customerId, CustomerRegisteredV1::buyerId)
```

Provide a default value for a property that didn't exist before — as used
by `OrderPlacedMigration` above:

<!-- validate: skip -->

```kotlin
builder.defaultValue(OrderPlacedV2::currency, "USD")
```

Split a source property into a target property by index after splitting on
a separator:

<!-- validate: declarations -->

```kotlin
@EventType(id = "customer-named", generation = 2)
data class CustomerNamedV2(val firstName: String)

@EventType(id = "customer-named", generation = 1)
data class CustomerNamedV1(val fullName: String)
```

<!-- validate: skip -->

```kotlin
// Inside upcast(builder: EventTypeMigrationBuilder<CustomerNamedV2, CustomerNamedV1>):
builder.split(
    CustomerNamedV2::firstName,
    CustomerNamedV1::fullName,
    separator = " ",
    part = 0
)
```

Combine multiple source properties into a target property — the reverse of
`split`, typically used in `downcast`:

<!-- validate: declarations -->

```kotlin
@EventType(id = "customer-named", generation = 3)
data class CustomerNamedV3(val firstName: String, val lastName: String)
```

<!-- validate: skip -->

```kotlin
// Inside downcast(builder: EventTypeMigrationBuilder<CustomerNamedV2, CustomerNamedV3>):
builder.combine(
    CustomerNamedV2::fullName,
    separator = " ",
    CustomerNamedV3::firstName,
    CustomerNamedV3::lastName
)
```

## Registering migrations

Pass the migration class alongside your `@EventType` classes to
`eventTypes.register` — Chronicle discovers `IEventTypeMigration`
implementations by reflection and merges them into the same registration as
the event types they migrate between:

<!-- validate: body needs=store -->

```kotlin
store.eventTypes.register(
    OrderPlacedV1::class,
    OrderPlacedV2::class,
    OrderPlacedMigration::class
)
```

A migration class discovered this way needs a public no-argument
constructor.

## Best practices

- Add a migration whenever you bump an event type's `generation` — without
  one, consumers still reading the old generation stop understanding new
  events (and vice versa).
- Keep each generation's shape additive where you can; use `defaultValue`
  for genuinely new properties instead of reaching for `split`/`combine`.
- Register migrations next to the event types they migrate between so the
  relationship between generations stays discoverable.
