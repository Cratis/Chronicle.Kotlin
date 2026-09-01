```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "event-processing-async-recorded")
data class EventProcessingAsyncRecorded(val value: Double)

@EventType(id = "event-processing-async-recorded-with-context")
data class EventProcessingAsyncRecordedWithContext(val value: Double)

@ReadModel
data class EventProcessingAsyncStats(val total: Double = 0.0)

@Reducer
class EventProcessingAsyncStatsReducer {
    // Async without context
    suspend fun recorded(event: EventProcessingAsyncRecorded, current: EventProcessingAsyncStats?): EventProcessingAsyncStats =
        EventProcessingAsyncStats((current?.total ?: 0.0) + event.value)

    // Async with context
    suspend fun recordedWithContext(
        event: EventProcessingAsyncRecordedWithContext,
        current: EventProcessingAsyncStats?,
        context: EventContext
    ): EventProcessingAsyncStats =
        EventProcessingAsyncStats((current?.total ?: 0.0) + event.value)
}
```
