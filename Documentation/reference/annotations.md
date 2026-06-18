# Annotations

## @EventType

Marks a data class as a Chronicle event type.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to class name. |
| `generation` | `Int` | `1` | Schema version. Increment when shape changes. |
| `tombstone` | `Boolean` | `false` | Signals event source retirement. |

```kotlin
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

```kotlin
@Reactor
class OrderNotifications { ... }
```

Supply an explicit `id` only when you need the identifier to survive class renames.

---

## @Reducer

Marks a class as a reducer. Each public method folds one event type into the
read model.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to class name. |

```kotlin
@Reducer
class OrderSummaryReducer { ... }
```

---

## @ReadModel

Marks a data class as a Chronicle read model.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to class name. |
| `displayName` | `String` | `""` | Human-readable label. Defaults to name. |

```kotlin
@ReadModel
data class OrderSummary(val orderId: String = "", val status: String = "pending")
```

---

## @Projection

Marks a class as a Chronicle projection. The class must implement
`IProjectionFor<T>` or be used with declarative field annotations.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to class name. |
| `readModel` | `KClass<*>` | `Any::class` | From `IProjectionFor<T>` |

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
annotated fields at rest using a per-subject key.

```kotlin
@EventType
data class CustomerRegistered(
    val customerId: String,
    @Pii val email: String
)
```

---

## @FromEvent

Applied to a read model class to declare that its fields are mapped from an
event type. Part of the annotation-based projection style.

| Parameter | Type | Description |
| --- | --- | --- |
| `eventType` | `KClass<*>` | The source event class. |
| `key` | `String` | Event property key. Default: `"EventSourceId"`. |

---

## @SetFrom

Applied to a read model property to declare which event field populates it.

| Parameter | Type | Description |
| --- | --- | --- |
| `propertyPath` | `String` | Event field name. Defaults to the property name. |
