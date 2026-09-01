```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.observation.EventSourceType
import io.cratis.chronicle.observation.EventStreamType
import io.cratis.chronicle.observation.FilterEventsByTag
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class ReducersFilteringCombineOrderPlaced(val totalAmount: Double)

@ReadModel
data class ReducersFilteringPremiumFulfillmentTotals(val count: Int = 0, val total: Double = 0.0)

class ReducersFilteringCombineOrderService(private val eventLog: IEventLog) {
    suspend fun placePremiumOrder(eventSourceId: String, totalAmount: Double) =
        eventLog.append(
            eventSourceId,
            ReducersFilteringCombineOrderPlaced(totalAmount),
            AppendOptions(tags = listOf("premium"), eventSourceType = "order", eventStreamType = "fulfillment")
        )
}

@Reducer
@FilterEventsByTag("premium")
@EventSourceType("order")
@EventStreamType("fulfillment")
class ReducersFilteringPremiumFulfillmentTotalsReducer {
    fun placed(event: ReducersFilteringCombineOrderPlaced, current: ReducersFilteringPremiumFulfillmentTotals?, context: EventContext) =
        ReducersFilteringPremiumFulfillmentTotals((current?.count ?: 0) + 1, (current?.total ?: 0.0) + event.totalAmount)
}
```
