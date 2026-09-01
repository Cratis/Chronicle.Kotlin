```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.observation.EventStreamType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "reducers-filtering-shipment-sent")
data class ReducersFilteringShipmentSent(val shippingCost: Double)

@ReadModel
data class ReducersFilteringShippingTotals(val count: Int = 0, val totalCost: Double = 0.0)

class ReducersFilteringShippingService(private val eventLog: IEventLog) {
    suspend fun send(eventSourceId: String, shippingCost: Double) =
        eventLog.append(
            eventSourceId,
            ReducersFilteringShipmentSent(shippingCost),
            AppendOptions(eventStreamType = "shipping")
        )
}

@Reducer
@EventStreamType("shipping")
class ReducersFilteringShippingTotalsReducer {
    fun sent(event: ReducersFilteringShipmentSent, current: ReducersFilteringShippingTotals?, context: EventContext) =
        ReducersFilteringShippingTotals((current?.count ?: 0) + 1, (current?.totalCost ?: 0.0) + event.shippingCost)
}
```
