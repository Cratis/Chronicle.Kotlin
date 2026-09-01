```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.observation.Replay

@EventType
data class ReplayAwareOrderPlaced(val orderId: String)

@Reactor
class ReplayAwareOrderReactor {
    fun sendConfirmation(event: ReplayAwareOrderPlaced) {
        // Runs as the event happens.
    }

    @Replay
    fun rebuildProjectionCache(event: ReplayAwareOrderPlaced) {
        // Runs instead of sendConfirmation while the observer is replaying.
    }
}
```
