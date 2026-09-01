```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import java.time.Instant

@EventType
data class PassiveReducersDataRecorded(val value: Double)

@ReadModel
data class PassiveReducersAnalytics(
    val recordCount: Int = 0,
    val totalValue: Double = 0.0,
    val lastUpdated: Instant = Instant.EPOCH
)

@Reducer(isActive = false)
class PassiveReducersTemporaryAnalyticsReducer {
    fun recorded(
        event: PassiveReducersDataRecorded,
        current: PassiveReducersAnalytics?,
        context: EventContext
    ): PassiveReducersAnalytics = PassiveReducersAnalytics(
        recordCount = (current?.recordCount ?: 0) + 1,
        totalValue = (current?.totalValue ?: 0.0) + event.value,
        lastUpdated = context.occurred
    )
}
```
