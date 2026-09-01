```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import java.time.Instant

@EventType(id = "event-processing-customer-action")
data class EventProcessingCustomerAction(val type: String, val description: String)

data class EventProcessingActivity(val type: String, val timestamp: Instant, val description: String)

@ReadModel
data class EventProcessingCustomerActivityLog(val activities: List<EventProcessingActivity> = emptyList())

@Reducer
class EventProcessingCustomerActivityLogReducer {
    fun recorded(
        event: EventProcessingCustomerAction,
        current: EventProcessingCustomerActivityLog?,
        context: EventContext
    ): EventProcessingCustomerActivityLog {
        // Copy rather than mutate - current.activities may still be referenced by a held snapshot
        val activities = (current?.activities ?: emptyList()) +
            EventProcessingActivity(event.type, context.occurred, event.description)

        return EventProcessingCustomerActivityLog(activities)
    }
}
```
