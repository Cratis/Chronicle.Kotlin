```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class EventProcessingMinimalMetricRecorded(val value: Double)

@ReadModel
data class EventProcessingMinimalStats(val count: Int = 0, val sum: Double = 0.0)

@Reducer
class EventProcessingMinimalStatsReducer {
    // Efficient - only creates a new object when needed
    fun recorded(event: EventProcessingMinimalMetricRecorded, current: EventProcessingMinimalStats?): EventProcessingMinimalStats {
        if (current == null) return EventProcessingMinimalStats(count = 1, sum = event.value)

        return current.copy(count = current.count + 1, sum = current.sum + event.value)
    }
}
```
