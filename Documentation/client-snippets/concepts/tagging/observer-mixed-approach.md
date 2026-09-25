```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.observation.Tag

// Kotlin exposes only @Tag (repeatable) - combine a multi-value use with a stacked one however reads best
@Tag("Notifications", "SMS")
@Tag("Customer")
@Reactor
class TaggingSmsNotificationReactor {
    fun on(event: TaggingSmsRequested) { }
}

@EventType
data class TaggingSmsRequested(val id: String = "")
```
