```kotlin
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventType

@EventType(id = "tagging-user-logged-in")
data class TaggingUserLoggedIn(val userId: String)

class TaggingUserLoginService(private val eventLog: IEventLog) {
    suspend fun recordLogin(eventSourceId: String) {
        eventLog.append(
            eventSourceId,
            TaggingUserLoggedIn("user123"),
            AppendOptions(tags = listOf("production", "critical"))
        )
    }
}
```
