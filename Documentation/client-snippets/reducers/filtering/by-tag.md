```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.observation.FilterEventsByTag
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class ReducersFilteringByTagOrderPlaced(val totalAmount: Double)

@ReadModel
data class ReducersFilteringPriorityOrderTotals(val count: Int = 0, val total: Double = 0.0)

class ReducersFilteringByTagOrderService(private val eventLog: IEventLog) {
    suspend fun placePriorityOrder(eventSourceId: String, totalAmount: Double) =
        eventLog.append(
            eventSourceId,
            ReducersFilteringByTagOrderPlaced(totalAmount),
            AppendOptions(tags = listOf("priority"))
        )
}

@Reducer
@FilterEventsByTag("priority")
class ReducersFilteringPriorityOrderTotalsReducer {
    fun placed(event: ReducersFilteringByTagOrderPlaced, current: ReducersFilteringPriorityOrderTotals?, context: EventContext) =
        ReducersFilteringPriorityOrderTotals((current?.count ?: 0) + 1, (current?.total ?: 0.0) + event.totalAmount)
}
```
