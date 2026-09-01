```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.FilterEventsByTag
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "reducers-filtering-multi-tag-order-placed")
data class ReducersFilteringMultiTagOrderPlaced(val totalAmount: Double)

@ReadModel
data class ReducersFilteringFastTrackOrderTotals(val count: Int = 0)

@Reducer
@FilterEventsByTag("priority")
@FilterEventsByTag("express")
class ReducersFilteringFastTrackOrderTotalsReducer {
    fun placed(event: ReducersFilteringMultiTagOrderPlaced, current: ReducersFilteringFastTrackOrderTotals?, context: EventContext) =
        ReducersFilteringFastTrackOrderTotals((current?.count ?: 0) + 1)
}
```
