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

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.observation.Reactor

@Reactor
class OrderNotifications {
    fun orderPlaced(event: OrderPlaced) {
        println("Order ${event.orderId} placed")
    }
}
```

Supply an explicit `id` only when you need the identifier to survive class renames.

---

## @Reducer

Marks a class as a reducer. Each public method folds one event type into the
read model.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to class name. |

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.observation.Reducer

@Reducer
class OrderSummaryReducer {
    fun orderPlaced(event: OrderPlaced, state: OrderSummary?): OrderSummary =
        (state ?: OrderSummary()).copy(orderId = event.orderId)
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
