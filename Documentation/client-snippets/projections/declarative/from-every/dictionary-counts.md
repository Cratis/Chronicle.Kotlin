```kotlin title="Declarative FromEvery with Dictionary Counting"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class OrderPlaced(val customerId: String, val amount: Double)

@EventType
data class OrderCancelled(val customerId: String)

@EventType
data class PaymentReceived(val customerId: String, val amount: Double)

data class EventStatistics(
    val id: String = "",
    val eventCountByType: MutableMap<String, Long> = mutableMapOf(),
    val eventCountByCorrelation: MutableMap<String, Long> = mutableMapOf()
)

class EventStatisticsProjection : IProjectionFor<EventStatistics> {
    override fun define(builder: IProjectionBuilderFor<EventStatistics>) {
        builder
            .from(OrderPlaced::class)
            .from(OrderCancelled::class)
            .from(PaymentReceived::class)
            .fromAll { feb ->
                feb.count(EventStatistics::eventCountByType, "eventType.id")
                feb.count(EventStatistics::eventCountByCorrelation, "correlationId")
            }
    }
}
```

This projection demonstrates `fromAll` dictionary counting, where:

- `eventCountByType` tracks how many events of each type have occurred (keyed by the event type identifier from `EventContext`)
- `eventCountByCorrelation` tracks event counts grouped by their correlation identifier

The dictionary key is resolved from an `EventContext` property for every event the projection observes. The supported operations are:

- `count(property, keyFromContext)` — increment the counter for the resolved key (same as `increment`)
- `increment(property, keyFromContext)` — add 1 to the value for the resolved key
- `decrement(property, keyFromContext)` — subtract 1 from the value for the resolved key

Common `EventContext` properties for dictionary keys:

- `"eventType.id"` — the event type identifier
- `"correlationId"` — the correlation identifier
- `"causedBy"` — who/what caused the event
- `"causation.size"` — number of causation chain entries

The dictionary property must be a `MutableMap<String, Long>` (or compatible type), and the key is always resolved from the event context, not from the event payload itself.
