```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class EventProcessingBasicSyncRecorded(val value: Double)

@ReadModel
data class EventProcessingBasicSyncStats(val total: Double = 0.0)

@Reducer
class EventProcessingBasicSyncStatsReducer {
    // Process event and return new state
    fun recorded(event: EventProcessingBasicSyncRecorded, current: EventProcessingBasicSyncStats?): EventProcessingBasicSyncStats =
        EventProcessingBasicSyncStats((current?.total ?: 0.0) + event.value)
}
```
