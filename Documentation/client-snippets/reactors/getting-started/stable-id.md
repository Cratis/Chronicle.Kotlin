```kotlin
import io.cratis.chronicle.observation.Reactor

@Reactor(id = "order-notifications")
class NamedOrderNotificationsReactor {
    fun placed(event: ReactorOrderPlaced) {
        // Perform the side effect.
    }
}
```
