```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "event-processing-pattern-with-context-recorded")
data class EventProcessingPatternWithContextRecorded(val value: Double)

@ReadModel
data class EventProcessingPatternWithContextStats(val total: Double = 0.0, val lastUpdated: java.time.Instant = java.time.Instant.EPOCH)

@Reducer
class EventProcessingPatternWithContextStatsReducer {
    // Access occurred time, correlation id, etc. via the third parameter
    fun recorded(
        event: EventProcessingPatternWithContextRecorded,
        current: EventProcessingPatternWithContextStats?,
        context: EventContext
    ): EventProcessingPatternWithContextStats =
        EventProcessingPatternWithContextStats((current?.total ?: 0.0) + event.value, context.occurred)
}
```
