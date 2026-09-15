```kotlin title="Model-Bound Dictionary Counting"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.CountFromAll
import io.cratis.chronicle.projections.DecrementFromAll
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.FromEventSourceId
import io.cratis.chronicle.projections.IncrementFromAll
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class OrderPlacedModelBound(val customerId: String, val amount: Double)

@EventType
data class OrderCancelledModelBound(val customerId: String)

@EventType
data class PaymentReceivedModelBound(val customerId: String, val amount: Double)

@ReadModel
@FromEvent(OrderPlacedModelBound::class)
@FromEvent(OrderCancelledModelBound::class)
@FromEvent(PaymentReceivedModelBound::class)
data class EventStatisticsModelBound(
    @FromEventSourceId
    val id: String = "",
    
    @CountFromAll(keyFromContext = "eventType.id")
    val eventCountByType: MutableMap<String, Long> = mutableMapOf(),
    
    @IncrementFromAll(keyFromContext = "correlationId")
    val eventCountByCorrelation: MutableMap<String, Long> = mutableMapOf(),
    
    @DecrementFromAll(keyFromContext = "causedBy")
    val reversalsByUser: MutableMap<String, Long> = mutableMapOf()
)
```

This model-bound projection demonstrates dictionary counting using annotations:

- `@CountFromAll(keyFromContext = "...")` — counts every event, incrementing the counter for the dictionary key resolved from the specified `EventContext` property
- `@IncrementFromAll(keyFromContext = "...")` — same as `@CountFromAll`, adds 1 to the value for the resolved key
- `@DecrementFromAll(keyFromContext = "...")` — subtracts 1 from the value for the resolved key

The dictionary property must be a `MutableMap<String, Long>` (or compatible type). The key is always resolved from an `EventContext` property, not from the event payload.

Common `EventContext` properties for dictionary keys:

- `"eventType.id"` — the event type identifier
- `"correlationId"` — the correlation identifier
- `"causedBy"` — who/what caused the event
- `"causation.size"` — number of causation chain entries

These annotations apply to all event types the projection observes via `@FromEvent`.
