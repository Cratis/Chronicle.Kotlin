```kotlin
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventType

@EventType
data class TaggingDynamicTagsEventOccurred(val data: String)

class TaggingDynamicTagsService(private val eventLog: IEventLog) {
    suspend fun recordProductionCritical(eventSourceId: String) =
        eventLog.append(
            eventSourceId,
            TaggingDynamicTagsEventOccurred("production issue"),
            AppendOptions(tags = listOf("production", "critical"))
        )

    suspend fun recordDevelopmentTest(eventSourceId: String) =
        eventLog.append(
            eventSourceId,
            TaggingDynamicTagsEventOccurred("test run"),
            AppendOptions(tags = listOf("development", "testing"))
        )

    suspend fun recordBatchMigration(eventSourceId: String) =
        eventLog.append(
            eventSourceId,
            TaggingDynamicTagsEventOccurred("batch migration"),
            AppendOptions(tags = listOf("migration", "batch-process"))
        )
}
```
