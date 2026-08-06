# Annotations

## @EventType

Marks a data class as a Chronicle event type.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to class name. |
| `generation` | `Int` | `1` | Schema version. Increment when shape changes. |
| `tombstone` | `Boolean` | `false` | Signals event source retirement. |

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class OrderPlaced(val orderId: String, val totalAmount: Double)
```

Omitting `id` is the common case — Chronicle uses `OrderPlaced` as the
identifier automatically.

---

## @Reactor

Marks a class as a Chronicle reactor. Each public method becomes a handler
for the event type of its first parameter.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to class name. |
| `eventSequence` | `String` | event log | The event sequence to observe. |

A handler takes the event, and optionally an `EventContext` carrying the event's
metadata:

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reactor

@Reactor
class OrderNotifications {
    fun orderPlaced(event: OrderPlaced) {
        println("Order ${event.orderId} placed")
    }

    fun orderShipped(event: OrderShipped, context: EventContext) {
        println("Order ${event.orderId} shipped at ${context.occurred}")
    }
}
```

Supply an explicit `id` only when you need the identifier to survive class renames.

---

## @OnceOnly

Excludes a reactor, or a single handler, from replay. Put it on the class and the
whole reactor is registered as non-replayable, so redaction, revision, and observer
rewind never replay it. Put it on one method and only that handler is skipped when
an event arrives as part of a replay — the reactor's other handlers still replay.

Use it for side effects where running again is worse than never running again.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.observation.OnceOnly
import io.cratis.chronicle.observation.Reactor

@Reactor
class PaymentNotifications {
    @OnceOnly
    fun orderPlaced(event: OrderPlaced) {
        println("Charging for ${event.orderId} - never repeated on replay")
    }
}
```

---

## @Replay

Marks a reactor handler as the one to run while events are being replayed. When
an event type has a handler marked with this, it takes over for the duration of
the replay and the everyday handler does not also run. Without one, the everyday
handler keeps running during replay.

Use [@OnceOnly](#onceonly) instead when the side effect should simply not happen
again on replay.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.observation.Replay

@Reactor
class ShippingNotifications {
    fun orderPlaced(event: OrderPlaced) {
        println("Emailing the customer about ${event.orderId}")
    }

    @Replay
    fun orderPlacedDuringReplay(event: OrderPlaced) {
        println("Rebuilding ${event.orderId} without emailing anyone")
    }
}
```

---

## @Reducer

Marks a class as a reducer. Each public method folds one event type into the
read model.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to class name. |
| `eventSequence` | `String` | event log | The event sequence to observe. |
| `isActive` | `Boolean` | `true` | Whether the kernel runs the reducer. |

A handler takes the event, the state so far, and optionally an `EventContext`.
The state is `null` until the first event for an event source has been folded in.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reducer

@Reducer
class OrderSummaryReducer {
    fun orderPlaced(event: OrderPlaced, state: OrderSummary?): OrderSummary =
        (state ?: OrderSummary()).copy(orderId = event.orderId)

    fun orderShipped(
        event: OrderShipped,
        state: OrderSummary?,
        context: EventContext
    ): OrderSummary = (state ?: OrderSummary()).copy(status = "shipped at ${context.occurred}")
}
```

---

## @ReadModel

Marks a data class as a Chronicle read model.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to class name. |
| `displayName` | `String` | `""` | Human-readable label. Defaults to name. |

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class OrderSummary(val orderId: String = "", val status: String = "pending")
```

---

## @Projection

Marks a class as a Chronicle projection, or overrides the projection
identifier on a model-bound read model. It is optional — when omitted, the
class simple name is used as the identifier.

For a declarative projection the read model type is inferred from the
`IProjectionFor<T>` type parameter. For a model-bound projection the
annotated class is itself the read model.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to class name. |
| `eventSequence` | `String` | event log | The event sequence to observe. |

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.Projection
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
@Projection(eventSequence = "outbox")
@FromEvent(OrderPlaced::class)
data class OutboxOrderTracking(val orderId: String = "")
```

---

## @Constraint

Marks a class as a Chronicle constraint definition. The class must implement
`IConstraint`.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to class name. |

---

## @Seeder

Marks a class as a Chronicle event seeder. The class must implement `ICanSeedEvents`.

---

## @Pii

Marks a property as personally identifiable information. Chronicle encrypts
annotated fields at rest using a per-subject key. See [PII
Attribute](/chronicle/compliance/pii/) for the full compliance model this
participates in.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `description` | `String` | `""` | Note about what the field holds. |

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.events.EventType

@EventType
data class CustomerRegistered(
    val customerId: String,
    @Pii(description = "Customer email address") val email: String
)
```

---

## @FromEvent

Applied to a read model class to declare that its fields are mapped from an
event type. Part of the annotation-based projection style. It is repeatable
— apply it once per event type the read model projects from.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `eventType` | `KClass<*>` | *(required)* | The source event class. |
| `key` | `String` | `"EventSourceId"` | Correlates events to instances. |

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class OrderShipped(val orderId: String, val carrier: String)

@ReadModel
@FromEvent(OrderPlaced::class)
@FromEvent(OrderShipped::class)
data class OrderTracking(
    val orderId: String = "",
    val status: String = ""
)
```

---

## @SetFrom

Applied to a read model property to override auto-mapping by name and
declare which event field populates it. It is repeatable, so one property
can be mapped differently per event type.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `propertyPath` | `String` | `""` | Path to the source property. |
| `eventType` | `KClass<*>` | `Nothing::class` | Event it applies to. |

`propertyPath` is a dot-separated path on the event, and defaults to the
annotated property's own name. The default `eventType` applies the mapping
to every event in the read model's `@FromEvent` list that has a matching
source property.

---

## @Join

Populates a read model property by joining against another event type on
its event source id. Use it when the triggering event doesn't carry the
read model's own key but instead references another entity by id.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `eventType` | `KClass<*>` | — | The event class to join against. |
| `on` | `String` | `""` | Read model property to join on. |
| `eventPropertyName` | `String` | `""` | Property on `eventType` to read. |

`on` and `eventPropertyName` both default to the annotated property's own name.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.projections.Join
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class OrderWithCustomerEmail(
    val orderId: String = "",
    val customerId: String = "",
    @Join(
        eventType = CustomerRegistered::class,
        on = "customerId",
        eventPropertyName = "email"
    )
    val customerEmail: String = ""
)
```

---

## @ChildrenFrom

Declares that a collection property is populated with child read model
instances created or updated by a specific event type.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `eventType` | `KClass<*>` | — | The event class that creates children. |
| `key` | `String` | `"EventSourceId"` | Event property identifying the child. |
| `identifiedBy` | `String` | `""` | Child's own identity property. |
| `parentKey` | `String` | `"EventSourceId"` | Event property for the parent. |

`identifiedBy` defaults to the child type's `id`/`key` property, falling back
to `EventSourceId`.

---

## @Nested

Marks a single nullable property as a nested sub-object built from its own
type's `@FromEvent`/`@SetFrom` annotations.

No parameters.

---

## @ClearWith

Declares which event clears (nulls out) a `@Nested` property. Placed on the
nested type itself, alongside its `@FromEvent` annotation.

| Parameter | Type | Description |
| --- | --- | --- |
| `eventType` | `KClass<*>` | The event class that clears the nested object. |

---

## @Count

Turns a property into an occurrence counter for a specific event type —
every time `eventType` fires for the read model instance, the property is
bumped by one.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `eventType` | `KClass<*>` | — | The event class to count occurrences of. |
| `constantKey` | `String` | `""` | See "Constant keys" below. |

---

## @Increment / @Decrement

Bumps a numeric property up (`@Increment`) or down (`@Decrement`) by one
every time `eventType` fires for the read model instance.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `eventType` | `KClass<*>` | — | The event class that bumps the property. |
| `constantKey` | `String` | `""` | See "Constant keys" below. |

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.projections.Increment
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class OrderShipmentStats(
    val orderId: String = "",
    @Increment(OrderShipped::class) val shipmentCount: Int = 0
)
```

**Constant keys:** when `constantKey` is set on `@Count`, `@Increment`, or
`@Decrement`, every occurrence of `eventType` updates the same read model
instance, identified by that constant value, instead of the projection's
normal per-instance key resolution.

---

## @AddFrom / @SubtractFrom

Adds (`@AddFrom`) or subtracts (`@SubtractFrom`) the value of an event
property into/from a numeric property every time `eventType` fires.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `eventType` | `KClass<*>` | — | The event class carrying the value. |
| `eventPropertyName` | `String` | `""` | Property on `eventType` to read. |

`eventPropertyName` defaults to the annotated property's own name.

---

## @FromAll / @FromEvery

Projects a property from every event type the projection observes, rather
than a single one. `@FromAll` and `@FromEvery` are equivalent aliases.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `property` | `String` | `""` | Triggering event property to read from. |
| `contextProperty` | `String` | `""` | Event context property to read from. |

Both default to the annotated property's own name. `contextProperty` (e.g.
the causing identity) takes precedence over `property` when both are set.

---

## @NotRewindable

Marks a projection as forward-only — it cannot be rewound and replayed
from scratch. Placed on the read model class.

No parameters.

---

## @RemovedWith

Declares which event removes a read model instance, or — when placed on a
`@ChildrenFrom` property — which event removes a single child from that
collection.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `eventType` | `KClass<*>` | — | The event class that triggers removal. |
| `key` | `String` | `"EventSourceId"` | Event property for what to remove. |
| `parentKey` | `String` | `"EventSourceId"` | Event property for the parent. |

`parentKey` only applies when removing a single child from a
`@ChildrenFrom` collection.

---

## @RemovedWithJoin

Like `@RemovedWith`, but the removal event doesn't directly carry the id —
it's resolved via a join instead.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `eventType` | `KClass<*>` | — | The event class that triggers the removal. |
| `key` | `String` | `"EventSourceId"` | Property used in the join lookup. |

---

## @NoAutoMap

Disables AutoMap. Placed on a read model, `@ChildrenFrom` element, or
`@Nested` type, it disables AutoMap entirely for that type. Placed on a
single property, it excludes just that property from AutoMap while
siblings keep auto-mapping.

No parameters.

---

## @Webhook

Marks a class as a discoverable Chronicle webhook definition. The class
must implement `IWebhookDefiner`.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to class name. |
| `targetUrl` | `String` | — | The URL to send events to. |

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.webhooks.IWebhookDefiner
import io.cratis.chronicle.webhooks.IWebhookDefinitionBuilder
import io.cratis.chronicle.webhooks.Webhook

@Webhook(targetUrl = "https://hooks.example.com/orders")
class OrderPlacedWebhook : IWebhookDefiner {
    override fun define(builder: IWebhookDefinitionBuilder) {
        builder.withEventType(OrderPlaced::class)
    }
}
```
