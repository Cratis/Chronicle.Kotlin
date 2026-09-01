```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.observation.FilterEventsByTag
import io.cratis.chronicle.observation.Reducer

@EventType
data class FilterByTagOrderPlaced(val totalAmount: Double = 0.0)

data class FilterByTagPriorityOrderTotals(val totalAmount: Double = 0.0)

@FilterEventsByTag("priority")
@Reducer
class FilterByTagPriorityOrderTotalsReducer {
    fun orderPlaced(event: FilterByTagOrderPlaced, current: FilterByTagPriorityOrderTotals?): FilterByTagPriorityOrderTotals =
        FilterByTagPriorityOrderTotals((current?.totalAmount ?: 0.0) + event.totalAmount)
}

suspend fun placePriorityOrder(store: IEventStore, eventSourceId: String, totalAmount: Double) =
    store.eventLog.append(eventSourceId, FilterByTagOrderPlaced(totalAmount), AppendOptions(tags = listOf("priority")))
```
