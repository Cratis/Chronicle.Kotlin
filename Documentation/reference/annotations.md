---
title: Annotations
description: Look up every annotation in the Chronicle JVM client, with its parameters, defaults and the kind of class or member it applies to in Kotlin and Java code.
---

Use this reference to find an annotation's parameters, defaults, and where it
applies. Browse by concern:

- Events and schemas: [@EventType](#eventtype), [@JsonSchemaType](#jsonschematype).
- Observers and event sequences: [@Reactor](#reactor), [@OnceOnly](#onceonly),
  [@StartAtTail](#startattail), [@Replay](#replay), [@Reducer](#reducer),
  [@EventSequence](#eventsequence).
- Read models and projections: [@ReadModel](#readmodel), [@Passive](#passive),
  [@Projection](#projection), [@FromEvent](#fromevent), [@Key](#key),
  [@ContextKey](#contextkey).
- Projection mappings: [@SetFrom](#setfrom), [@SetValue](#setvalue),
  [@SetFromContext](#setfromcontext), [@Join](#join),
  [@ChildrenFrom](#childrenfrom), [@Nested](#nested), [@ClearWith](#clearwith),
  [@Count](#count), [@Increment / @Decrement](#increment--decrement),
  [@AddFrom / @SubtractFrom](#addfrom--subtractfrom),
  [@FromAll / @FromEvery](#fromall--fromevery), [@NotRewindable](#notrewindable),
  [@RemovedWith](#removedwith), [@RemovedWithJoin](#removedwithjoin),
  [@NoAutoMap](#noautomap).
- Graph projections: [@Graph](#graph), [@Node](#node), [@Edge](#edge),
  [@EdgeSource / @EdgeTarget](#edgesource--edgetarget),
  [@EdgeFrom](#edgefrom), [@EdgeRemovedWith](#edgeremovedwith),
  [@ViewOf](#viewof), [@Follows](#follows), [@FromEdge](#fromedge).
- Constraints: [@Constraint](#constraint), [@Unique](#unique),
  [@RemoveConstraint](#removeconstraint).
- Seeding, compliance and webhooks: [@Seeder](#seeder), [@Pii](#pii),
  [@Subject](#subject), [@Webhook](#webhook).

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

## @CompensationFor

Marks an `@EventType` as a compensation for another event type. The target is
recorded on the event type's schema, and `compensate` only accepts the event
when its type is declared for the type of the event being compensated.

| Parameter | Type | Description |
| --- | --- | --- |
| `eventType` | `KClass<*>` | The `@EventType` this event compensates for. |

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.CompensationFor
import io.cratis.chronicle.events.EventType

@EventType
data class InvoiceIssued(val invoiceId: String)

@EventType
@CompensationFor(InvoiceIssued::class)
data class InvoiceCredited(val reason: String)
```

See [Compensating events](../guides/compensation.md).

---

## @Reactor

Marks a class as a Chronicle reactor. Each public method becomes a handler
for the event type of its first parameter.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to class name. |
| `eventSequence` | `String` | event log | The event sequence to observe. Overridden by [@EventSequence](#eventsequence). |

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
Class-level `@OnceOnly` needs a Chronicle 19.33.0 or later kernel; an older kernel
ignores it and still replays the reactor.

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

## @StartAtTail

Marks a reactor class so it does not observe history that predates its first
registration. A new reactor normally catches up from the very beginning of the
event sequence, so one whose handlers cause side effects acts on every
historical event the moment it is deployed. [@OnceOnly](#onceonly) does not
prevent that, because it only excludes replay. With `@StartAtTail`, the first
time Chronicle sees the reactor it begins at the tail of the event sequence and
observes only what is appended afterwards.

It only affects the first registration. A reactor that already has a position
keeps it, so the annotation never skips events after a restart. Renaming the
reactor or moving its package changes its default id and makes it a new reactor,
which starts at the tail again; give `@Reactor` an explicit `id` to keep the old
position. An explicit replay still replays from the beginning. The Chronicle
server must also support it: a server that predates it ignores the request and
the reactor catches up from the beginning.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.observation.StartAtTail

@Reactor
@StartAtTail
class NewOrderNotifications {
    fun orderPlaced(event: OrderPlaced) {
        println("Notifying ${event.orderId} - only for new orders")
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

## @Observes

Declares an event type a **batch method** observes. A batch method takes an
`EventBatch` as its first parameter and is called once per delivered page with
every event of the types it observes, in sequence order and across event
sources, rather than once per event. Repeat the annotation once per event type
and match on the events inside. Java writes it the same way:
`@Observes(OrderPlaced.class)`.

A reactor batch method takes the `EventBatch` and optionally a `BatchContext`,
and returns nothing. A reactor can have batch methods and per-event handlers
side by side, but each event type reaches exactly one method: observing a type
another method also handles, `@Observes` on a method without an `EventBatch`,
a batch method without `@Observes`, or a per-event handler taking a
`BatchContext` is refused at registration.

`batch.fail(...)` and `batch.defer(...)` report a single event source - from its
first event in the batch, or from a given event - and the rest of the batch
succeeds. Throwing fails every event source in the batch. See
[Handling events in batches](/chronicle/reactors/batches/).

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.observation.BatchContext
import io.cratis.chronicle.observation.EventBatch
import io.cratis.chronicle.observation.Observes
import io.cratis.chronicle.observation.Reactor
import java.time.Duration

@Reactor
class ShipmentIndex {
    @Observes(OrderPlaced::class)
    @Observes(OrderShipped::class)
    suspend fun index(batch: EventBatch, context: BatchContext) {
        for (e in batch) {
            when (val event = e.event) {
                is OrderPlaced -> println("Indexing ${event.orderId} (${context.origin})")
                is OrderShipped ->
                    if (event.carrier.isEmpty()) batch.defer(e.context, Duration.ofSeconds(30))
            }
        }
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
| `eventSequence` | `String` | event log | The event sequence to observe. Overridden by [@EventSequence](#eventsequence). |
| `isActive` | `Boolean` | `true` | Whether the kernel runs the reducer. |
| `maxConcurrency` | `Int` | `0` | Event sources reduced at once. |

The event sources of one delivered page are reduced at the same time, up to
`maxConcurrency`. `0` uses the processor count, and `1` reduces them one after
another. The calls share the reducer instance.

A passive reducer (`isActive = false`) stores nothing: reading its read model
folds the event source's events from the reducer's event sequence in the
client on each read, so the result is current. The cost is a read of those
events on every call. A reducer whose read model carries
[@Passive](#passive) is passive too.

A handler takes the event, the state so far, and optionally an `EventContext`.
The state is `null` until the first event for an event source has been folded in.

Instead of per-event handlers, a reducer can have exactly one batch method -
never both. It takes an `EventBatch` of one event source's events, the state so
far, and optionally a `BatchContext`, declares its event types with
[@Observes](#observes), and returns the next state; `null` removes the read
model. It is called once per event source of each delivered page. See
[Reducing events in batches](/chronicle/reducers/batches/).

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

## @Passive

Marks a read model's projection as passive: registered with the kernel but
not actively run, and not stored. `getInstanceByKey` computes a passive read
model from its events on every read, so the result includes an event appended
immediately before. Use it for a read model that is read rarely or must be
current, and keep the default for one that is read often. Placed on the read
model class; applies to model-bound projections, to a fluent projection whose
read model carries it, and to the reducer that produces it. Requires client
6.5.0 or later.

A passive projection is computed from the events of the key you ask for, so
it suits projections keyed by event source id. A projection that takes its
key from an event property or joins events from other event sources needs the
stored, active form to be complete.

No parameters.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.Passive
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class SnapshotCreated(val data: String)

@Passive
@ReadModel
@FromEvent(SnapshotCreated::class)
data class HistoricalSnapshot(val data: String = "")
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
| `eventSequence` | `String` | event log | The event sequence to observe. Overridden by [@EventSequence](#eventsequence). |

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

## @EventSequence

Points an observer — a reactor, a reducer or a projection — at the event
sequence it observes. This is the standalone alternative to the
`eventSequence` parameter on [@Reactor](#reactor), [@Reducer](#reducer) and
[@Projection](#projection); reach for it when the sequence is the only thing
being configured, so the observer keeps its conventional identifier.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `value` | `String` | *(required)* | The event sequence to observe. |

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.observation.EventSequence
import io.cratis.chronicle.observation.Reactor

@Reactor
@EventSequence("outbox")
class OutboxOrderNotifications {
    fun orderPlaced(event: OrderPlaced) {
        println("Order ${event.orderId} placed, observed from the outbox")
    }
}
```

When both this annotation and the `eventSequence` parameter are present, this
annotation wins.

There is no `@EventLog` counterpart. In the .NET client it exists to override
the inbox routing that `[EventStore]` on an event type turns on — and the
Kotlin client has no `@EventStore`, so an observer without an explicit
sequence already reads from the event log.

---

## @Constraint

Marks a class as a Chronicle constraint definition. The class must implement
`IConstraint`.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Stable identifier. Defaults to class name. |

---

## @Unique

Marks a property or an event type as needing to be unique - the model-bound
alternative to a hand-written `IConstraint`. On a property, no two events of
that type may carry the same value; applying it with the same `id` to
properties on more than one event type groups them under one constraint,
checked across all of them combined. On an event type, at most one instance
of that type may exist per event source.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `id` | `String` | `""` | Constraint name, defaulting to the property/class. |
| `message` | `String` | `""` | Message for a constraint violation. |

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.constraints.Unique
import io.cratis.chronicle.events.EventType

@EventType
data class ProjectCreated(@Unique val name: String, val description: String)

@EventType
@Unique
data class WorkspaceClaimed(val slug: String)
```

Pair it with [@RemoveConstraint](#removeconstraint) on a removal event to
release the value for reuse.

---

## @RemoveConstraint

Marks an event type as releasing a named [@Unique](#unique) constraint when
it is appended - typically a deletion or lifecycle-ending event. Repeatable,
so one event can release more than one constraint.

Only one event type may release a given constraint name with this client -
if more than one declares the same name, registration keeps the first one
it finds and reports the rest, rather than silently overwriting on every
reconnect.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `value` | `String` | *(required)* | Name of the constraint to release. |

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.constraints.RemoveConstraint
import io.cratis.chronicle.events.EventType

@EventType
@RemoveConstraint("UniqueWorkspaceSlug")
data class WorkspaceArchived(val workspaceId: String)
```

---

## @Seeder

Marks a class as a Chronicle event seeder. The class must implement `ICanSeedEvents`.

---

## @Pii

Marks a property, constructor parameter, field, or type as personally
identifiable information. Chronicle encrypts annotated values at rest using a
per-subject key. See [PII Attribute](/chronicle/compliance/pii/) for the full
compliance model this participates in.

Applying it directly to a property works, but the declare-once pattern is to
put it on a `ConceptAs<T>` type instead: every event or read model property
that reuses that concept is PII automatically, with nothing to repeat at each
call site. It can also mark a composite value object type, in which case
every value the type holds is treated as PII wherever that type appears.

`@Pii` cannot be applied to an `EventSourceId` concept — Chronicle uses the
event source id to look up the encryption key for every other PII value
belonging to that source, so encrypting the id itself would make its own key
unfindable.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `description` | `String` | `""` | Note about what the field holds. |

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.compliance.Pii
import io.cratis.chronicle.events.EventType

// Declared once on the concept, every event or read model property that
// reuses EmailAddress is PII automatically.
@Pii(description = "Customer email address")
data class EmailAddress(val value: String) :
    io.cratis.fundamentals.concepts.ConceptAs<String> {
    override fun value(): String = value
}

@EventType
data class CustomerRegistered(
    val customerId: String,
    val email: EmailAddress
)
```

---

## @JsonSchemaType

Overrides the type a class is represented as in the generated JSON schema.
Apply it to a type that brings its own serializer and writes something other
than its own shape on the wire — a value object collapsed into a single
string, for instance. Without it the generated schema would describe the
Kotlin shape, and the value would not round-trip through the kernel.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `type` | `KClass<*>` | *(required)* | Type the class is represented as. |

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.schemas.JsonSchemaType

// Money serializes as a single string ("42.50 USD") through its own
// serializer, so the schema needs to describe a string, not an object
// with amount/currency fields.
@JsonSchemaType(String::class)
data class Money(val amount: Double, val currency: String)
```

Pointing the annotation at the annotated type itself throws
`SelfReferencingJsonSchemaType` — generating that schema would recurse
forever.

---

## @Subject

Marks a property as the compliance subject - the identity a release
decrypts [@Pii](#pii) values against. `IReadModelsService.release` uses it
to pick which property carries the subject; without it, release falls back
to a property named `id` (case-insensitive), the convention every read
model followed before this annotation existed.

Add it whenever a read model's subject is not its `id` - for example a
support ticket keyed by ticket id but holding a customer's PII, where the
customer, not the ticket, is who the encryption key belongs to.

No parameters.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.Subject
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class SupportTicketSummary(
    val id: String = "",
    @Subject val customerId: String = "",
    val topic: String = ""
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

## @Key

Marks a property on an event as the key a projection correlates that event
to a read model instance by. [@FromEvent](#fromevent)'s `key` parameter
takes this today as a bare property-name string; `@Key` is the
strongly-typed alternative for consumers that resolve the key by
reflection instead of by name.

On a [@Node](#node) it marks the property holding the node's unique key, and
on an [@Edge](#edge) the property that identifies the edge among edges of the
same type between the same two nodes.

No parameters.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.keys.Key

@EventType
data class PickTicketOpened(@Key val orderId: String, val warehouse: String)
```

---

## @ContextKey

Marks a function as deriving its key from the event context — for example
the event source id, or a correlation id — rather than from a property on
the event payload.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `property` | `String` | *(required)* | `EventContext` property to use. |

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.keys.ContextKey

class PickTicketHandlers {
    @ContextKey(property = "EventSourceId")
    fun pickTicketOpened(event: PickTicketOpened) = Unit
}
```

`IKeyBuilder`/`KeyBuilder` build the same resolution fluently instead of
declaratively — see the `io.cratis.chronicle.keys` package.

---

## @SetFrom

Applied to a read model property to override auto-mapping by name and
declare which event field populates it. It is repeatable, so one property
can be mapped differently per event type.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `propertyPath` | `String` | `""` | Path to the source property. |
| `eventType` | `KClass<*>` | `Nothing::class` | Event it applies to. |
| `setOnce` | `Boolean` | `false` | Set only while the property is empty. |

`propertyPath` is a dot-separated path on the event, and defaults to the
annotated property's own name. The default `eventType` applies the mapping
to every event in the read model's `@FromEvent` list that has a matching
source property.

With `setOnce = true` the first event that produces a value wins and later
events leave the property as it is. Replay gives the same result, because
the outcome depends only on the order of the events.

---

## @SetValue

Sets a read model property to a constant value when a specific event occurs,
or clears it back to no value. It is repeatable, so the same property can
hold a different constant per event type.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `eventType` | `KClass<*>` | — | The event class to trigger on. |
| `value` | `String` | `""` | The constant's literal text. Ignored if `clear`. |
| `clear` | `Boolean` | `false` | Clears instead of setting `value`. |

Kotlin annotation parameters cannot be nullable, so `value` is always a plain
string — a numeric or boolean constant is written out as its literal text
(`"42"`, `"true"`) rather than as a typed argument. Set `clear = true` to
clear the property instead, which is Kotlin's equivalent of passing `null`
for `value` in the .NET client.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetValue
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class SubscriptionActivated(val placeholder: Boolean = true)

@EventType
data class SubscriptionCanceled(val placeholder: Boolean = true)

@ReadModel
@FromEvent(SubscriptionActivated::class)
@FromEvent(SubscriptionCanceled::class)
data class Subscription(
    @SetValue(SubscriptionActivated::class, value = "active")
    @SetValue(SubscriptionCanceled::class, value = "canceled")
    val status: String = ""
)
```

---

## @SetFromContext

Maps a read model property from a named `EventContext` property, for one
specific event. Unlike [@FromAll / @FromEvery](#fromall--fromevery), which
map a context property across every event the projection observes, this ties
the mapping to a single event type.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `eventType` | `KClass<*>` | — | The event class this mapping applies to. |
| `contextProperty` | `String` | `""` | The context property to read from. |
| `setOnce` | `Boolean` | `false` | Set only while the property is empty. |

`contextProperty` defaults to the annotated property's own name. Use
`setOnce = true` to keep, for example, the time something was first seen.

<!-- validate: declarations -->

```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.SetFromContext
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class OrderPlacedForAudit(val customerName: String)

@ReadModel
@FromEvent(OrderPlacedForAudit::class)
data class AuditedOrder(
    @SetFrom("customerName", OrderPlacedForAudit::class)
    val customerName: String = "",

    @SetFromContext(OrderPlacedForAudit::class, contextProperty = "occurred")
    val orderedAt: String = ""
)
```

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
| `prepend` | `Boolean` | `false` | Put new children first. |
| `orderBy` | `String` | `""` | Sort children ascending by this. |
| `orderByDescending` | `String` | `""` | Sort children descending by this. |

`identifiedBy` defaults to the child type's `id`/`key` property, falling back
to `EventSourceId`.

Declare at most one of `prepend`, `orderBy` and `orderByDescending`. When a
property has several `@ChildrenFrom` annotations, each one that declares an
ordering must declare the same one; otherwise registration fails with
`ConflictingChildrenOrdering`. Children ordering needs Chronicle 19.150.0 or
later.

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
| `detach` | `Boolean` | `false` | Only on a [@Node](#node): delete the node's edges with it. |

`parentKey` only applies when removing a single child from a
`@ChildrenFrom` collection. `detach` only applies to a graph node; without
it a node that still has edges cannot be deleted.

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

## @Graph

Names the property graph a node or an edge belongs to, or a fluent
`IGraphProjection` builds. Without it the graph is named after the package of
the type. The name is the identifier of the read model the graph is registered
as. See [Graph projections](../guides/graph-projections.md).

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `value` | `String` | — | The name of the graph. |

---

## @Node

Marks a class as a node of a property graph. The label of the node is the
simple name of the class. Exactly one property needs [@Key](#key). Project
events onto it with [@FromEvent](#fromevent), [@RemovedWith](#removedwith) and
the property mappings of a read model. A node cannot also be a
[@ReadModel](#readmodel).

No parameters.

---

## @Edge

Marks a class as an edge of a property graph. Exactly one property needs
[@EdgeSource](#edgesource--edgetarget) and one
[@EdgeTarget](#edgesource--edgetarget). One property with [@Key](#key) gives
the edge an identity.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `value` | `String` | — | The type of the edge, such as `MEMBER_OF`. |

---

## @EdgeSource / @EdgeTarget

Mark the properties of an [@Edge](#edge) holding the keys of the node the edge
starts from and the node it ends at. They are not properties of the edge. They
are the `[Source<T>]` and `[Target<T>]` of the .NET client.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `value` | `KClass<*>` | — | The node class. |

---

## @EdgeFrom

Declares that an event creates or updates an [@Edge](#edge). Repeatable. The
properties of the edge map by name from the event, or with the property
mappings of a read model naming this event.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `eventType` | `KClass<*>` | — | The event class. |
| `sourceKey` | `String` | `"EventSourceId"` | Event property: source key. |
| `targetKey` | `String` | `"EventSourceId"` | Event property: target key. |
| `identity` | `String` | `""` | Event property: edge identity. |

`identity` defaults to the event property named like the edge's `@Key`.

---

## @EdgeRemovedWith

Declares that an event deletes an [@Edge](#edge). The nodes at both ends stay.
Repeatable, with the same parameters as [@EdgeFrom](#edgefrom) except that it
maps no properties.

---

## @ViewOf

Marks a class as a view over a property graph, anchored at one
[@Node](#node). The members of the view are its constructor properties, in
declaration order: properties of the node, relations marked
[@Follows](#follows) and edge properties marked [@FromEdge](#fromedge). The
property marked [@Key](#key) is the key of the node. The view cannot also be a
node or an edge. See [Graph projections](../guides/graph-projections.md#read-a-graph-with-views).

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `value` | `KClass<*>` | — | The node class the view is anchored at. |
| `name` | `String` | `""` | Unique view name. Defaults to the class name. |

---

## @Follows

Marks a member of a [@ViewOf](#viewof) view as a relation that follows one
[@Edge](#edge) to another view. A collection is a relation of cardinality many,
a single nullable value cardinality one. The direction is derived from the
labels of the edge; an edge that starts and ends at the same node label needs
an explicit `Outgoing` or `Incoming`.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `value` | `KClass<*>` | — | The edge class that is followed. |
| `direction` | `FollowDirection` | `Derived` | `Outgoing` or `Incoming`. |

---

## @FromEdge

Marks a member of a [@ViewOf](#viewof) view as a property of the edge that was
followed to reach the view. Only valid in a view that is the target of
relations of one edge type.

| Parameter | Type | Default | Description |
| --- | --- | --- | --- |
| `source` | `String` | `""` | Edge property name. Defaults to member name. |

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
