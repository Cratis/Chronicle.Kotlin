```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class EventProcessingMetricRecorded(val value: Double)

@ReadModel
data class EventProcessingStatistics(val sum: Double = 0.0, val count: Int = 0, val average: Double = 0.0)

@Reducer
class EventProcessingStatisticsReducer {
    fun recorded(event: EventProcessingMetricRecorded, current: EventProcessingStatistics?): EventProcessingStatistics {
        val sum = (current?.sum ?: 0.0) + event.value
        val count = (current?.count ?: 0) + 1
        return EventProcessingStatistics(sum, count, sum / count)
    }
}
```
