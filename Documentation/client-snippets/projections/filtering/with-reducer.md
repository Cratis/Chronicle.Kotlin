```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.FilterEventsByTag
import io.cratis.chronicle.observation.Reducer

data class FilteringPremiumOrderTotals(val count: Int = 0, val total: Double = 0.0)

@Reducer
@FilterEventsByTag("premium")
class FilteringPremiumOrderTotalsReducer {
    fun placed(
        event: FilteringWithReactorOrderPlaced,
        current: FilteringPremiumOrderTotals?,
        context: EventContext
    ): FilteringPremiumOrderTotals =
        FilteringPremiumOrderTotals(
            count = (current?.count ?: 0) + 1,
            total = (current?.total ?: 0.0) + event.totalAmount
        )
}
```
