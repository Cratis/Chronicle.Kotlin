```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class EventProcessingSkipItemAdded(val price: Double)

@ReadModel
data class EventProcessingSkipOrderSummary(val total: Double = 0.0)

@Reducer
class EventProcessingSkipOrderSummaryReducer {
    fun itemAdded(event: EventProcessingSkipItemAdded, current: EventProcessingSkipOrderSummary?, context: EventContext): EventProcessingSkipOrderSummary? {
        // Can't add items if order doesn't exist
        if (current == null) return null

        return current.copy(total = current.total + event.price)
    }
}
```
