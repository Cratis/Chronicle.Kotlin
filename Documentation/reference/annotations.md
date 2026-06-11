# Annotations

## @EventType

Marks a data class as a Chronicle event type.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable string identifier. Defaults to the simple class name when empty. |
| `generation` | `Int` | `1` | Schema version. Increment when the event shape changes. |
| `tombstone` | `Boolean` | `false` | When `true`, this event signals retirement of the event source. |

```kotlin
@EventType
data class OrderPlaced(val orderId: String, val totalAmount: Double)
```

Omitting `id` is the common case — Chronicle uses `OrderPlaced` as the identifier automatically.

---

## @Reactor

Marks a class as a Chronicle reactor. Each public method becomes a handler for the event type of its first parameter.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to the simple class name when empty. |

```kotlin
@Reactor
class OrderNotifications { ... }
```

Supply an explicit `id` only when you need the identifier to survive class renames.

---

## @Reducer

Marks a class as a reducer. Each public method folds one event type into the read model.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to the simple class name when empty. |

```kotlin
@Reducer
class OrderSummaryReducer { ... }
```

---

## @ReadModel

Marks a data class as a Chronicle read model.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to the simple class name when empty. |
| `displayName` | `String` | `""` | Human-readable label. Defaults to the simple class name when empty. |

```kotlin
@ReadModel
data class OrderSummary(val orderId: String = "", val status: String = "pending")
```

---

## @Projection

Marks a class as a Chronicle projection. The class must implement `IProjectionFor<T>` or be used with declarative field annotations.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to the simple class name when empty. |
| `readModel` | `KClass<*>` | `Any::class` | The read model type. Inferred from the `IProjectionFor<T>` type parameter when omitted. |

---

## @Constraint

Marks a class as a Chronicle constraint definition. The class must implement `IConstraint`.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to the simple class name when empty. |

---

## @Seeder

Marks a class as a Chronicle event seeder. The class must implement `ICanSeedEvents`.

---

## @Pii

Marks a property as personally identifiable information. Chronicle encrypts annotated fields at rest using a per-subject key.

```kotlin
@EventType
data class CustomerRegistered(
    val customerId: String,
    @Pii val email: String
)
```

---

## @FromEvent

Applied to a read model class to declare that its fields are mapped from an event type. Part of the annotation-based projection style.

| Parameter | Type | Description |
| --- | --- | --- |
| `eventType` | `KClass<*>` | The source event class. |
| `key` | `String` | The property on the event to use as the read model key (default: `"EventSourceId"`). |

---

## @SetFrom

Applied to a read model property to declare which event field populates it.

| Parameter | Type | Description |
| --- | --- | --- |
| `propertyPath` | `String` | The field name on the event (defaults to the annotated property's name when empty). |
