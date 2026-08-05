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
annotated fields at rest using a per-subject key. See [PII
Attribute](/chronicle/compliance/pii/) for the full compliance model this
participates in.

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

```kotlin
@Join(
    eventType = CustomerRegistered::class,
    on = "customerId",
    eventPropertyName = "fullName"
)
val customerName: String = ""
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

```kotlin
@Increment(EmployeePromoted::class) val promotionCount: Int = 0
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

```kotlin
@Webhook(targetUrl = "https://hooks.example.com/orders")
class OrderPlacedWebhook : IWebhookDefiner {
    override fun define(builder: IWebhookDefinitionBuilder) {
        builder.withEventType(OrderPlaced::class)
    }
}
```
