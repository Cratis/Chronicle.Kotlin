```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.observation.EventStreamType
import io.cratis.chronicle.observation.Reducer

@EventType
data class FilterByStreamTypeShipmentSent(val shippingCost: Double = 0.0)

data class FilterByStreamTypeShippingTotals(val shippingCost: Double = 0.0)

@EventStreamType("shipping")
@Reducer
class FilterByStreamTypeShippingTotalsReducer {
    fun shipmentSent(event: FilterByStreamTypeShipmentSent, current: FilterByStreamTypeShippingTotals?): FilterByStreamTypeShippingTotals =
        FilterByStreamTypeShippingTotals((current?.shippingCost ?: 0.0) + event.shippingCost)
}

suspend fun send(store: IEventStore, eventSourceId: String, shippingCost: Double) =
    store.eventLog.append(eventSourceId, FilterByStreamTypeShipmentSent(shippingCost), AppendOptions(eventStreamType = "shipping"))
```
