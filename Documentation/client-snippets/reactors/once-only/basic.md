```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.OnceOnly
import io.cratis.chronicle.observation.Reactor

@EventType
data class OnceOnlyOrderPlaced(val orderId: String)

@Reactor
class OnceOnlyOrderReactor {
    @OnceOnly
    fun sendNotification(event: OnceOnlyOrderPlaced) {
        // Runs on normal delivery, skipped during replay; keep the side effect idempotent because delivery can be retried.
    }
}
```
