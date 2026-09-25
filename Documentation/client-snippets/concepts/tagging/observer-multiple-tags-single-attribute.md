```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.observation.Tag

@Tag("Notifications", "Customer", "Email")
@Reactor
class TaggingCustomerNotificationReactor {
    fun on(event: TaggingCustomerRegistered) { }
}

@EventType
data class TaggingCustomerRegistered(val id: String = "")
```
