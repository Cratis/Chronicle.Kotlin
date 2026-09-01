```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class DecNotRewindableUserAction(
    val userId: String,
    val actionType: String,
    val details: String
)

@EventType
data class DecNotRewindableSystemEvent(
    val componentName: String,
    val eventType: String,
    val data: String
)
```
