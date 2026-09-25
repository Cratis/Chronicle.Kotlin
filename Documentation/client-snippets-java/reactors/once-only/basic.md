```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.OnceOnly;
import io.cratis.chronicle.observation.Reactor;

@EventType
record OnceOnlyOrderPlaced(String orderId) {}

@Reactor
class OnceOnlyOrderReactor {
    @OnceOnly
    void sendNotification(OnceOnlyOrderPlaced event) {
        // Runs on normal delivery, skipped during replay; keep the side effect idempotent because delivery can be retried.
    }
}
```
