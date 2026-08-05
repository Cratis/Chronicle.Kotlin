```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.OnceOnly
import io.cratis.chronicle.observation.Reactor

@EventType(id = "once-only-order-placed")
data class OnceOnlyOrderPlaced(val orderId: String)

@Reactor
class OnceOnlyOrderReactor {
    @OnceOnly
    fun sendNotification(event: OnceOnlyOrderPlaced) {
        // Runs once when the event is first observed, and is skipped during replay.
    }
}
```
