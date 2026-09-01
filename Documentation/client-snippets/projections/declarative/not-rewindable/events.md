```kotlin
import io.cratis.chronicle.events.EventType

@EventType(id = "dec-not-rewindable-user-action")
data class DecNotRewindableUserAction(
    val userId: String,
    val actionType: String,
    val details: String
)

@EventType(id = "dec-not-rewindable-system-event")
data class DecNotRewindableSystemEvent(
    val componentName: String,
    val eventType: String,
    val data: String
)
```
