```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.observation.Replay;

@EventType
record ReplayAwareOrderPlaced(String orderId) {}

@Reactor
class ReplayAwareOrderReactor {
    void sendConfirmation(ReplayAwareOrderPlaced event) {
        // Runs as the event happens.
    }

    @Replay
    void rebuildProjectionCache(ReplayAwareOrderPlaced event) {
        // Runs instead of sendConfirmation while the observer is replaying.
    }
}
```
