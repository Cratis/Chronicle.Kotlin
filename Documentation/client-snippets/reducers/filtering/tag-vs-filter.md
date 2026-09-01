```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.EventSourceType
import io.cratis.chronicle.observation.FilterEventsByTag
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.observation.Tag
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class ReducersFilteringTagVsFilterOrderPlaced(val totalAmount: Double)

@ReadModel
data class ReducersFilteringTagVsFilterTotals(val count: Int = 0, val total: Double = 0.0)

// These labels appear on the reducer definition - they do not affect dispatch
@Reducer
@Tag("reporting")
@Tag("premium")
class ReducersFilteringLabeledFulfillmentTotalsReducer {
    fun placed(event: ReducersFilteringTagVsFilterOrderPlaced, current: ReducersFilteringTagVsFilterTotals?, context: EventContext) =
        ReducersFilteringTagVsFilterTotals((current?.count ?: 0) + 1, (current?.total ?: 0.0) + event.totalAmount)
}

// These filter which events are dispatched to the reducer
@Reducer
@FilterEventsByTag("premium")
@EventSourceType("order")
class ReducersFilteringFilteredFulfillmentTotalsReducer {
    fun placed(event: ReducersFilteringTagVsFilterOrderPlaced, current: ReducersFilteringTagVsFilterTotals?, context: EventContext) =
        ReducersFilteringTagVsFilterTotals((current?.count ?: 0) + 1, (current?.total ?: 0.0) + event.totalAmount)
}
```
