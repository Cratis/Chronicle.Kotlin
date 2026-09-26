```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.observation.Tag

@Tag("Notifications")
@Reactor
class TaggingOrderConfirmationReactor {
    fun on(event: TaggingOrderConfirmed) { }
}

@EventType
data class TaggingOrderConfirmed(val id: String = "")
```
