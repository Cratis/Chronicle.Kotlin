```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.observation.Tag
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "tagging-reducers-order-placed")
data class TaggingReducersOrderPlaced(val totalAmount: Double)

@ReadModel
data class TaggingReducersOrderAnalytics(val orderCount: Int = 0, val totalAmount: Double = 0.0)

@Reducer
@Tag("Analytics")
class TaggingReducersOrderAnalyticsReducer {
    fun placed(event: TaggingReducersOrderPlaced, current: TaggingReducersOrderAnalytics?, context: EventContext) =
        TaggingReducersOrderAnalytics((current?.orderCount ?: 0) + 1, (current?.totalAmount ?: 0.0) + event.totalAmount)
}
```
