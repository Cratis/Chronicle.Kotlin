```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.OnceOnly;
import io.cratis.chronicle.observation.Reactor;

@EventType(id = "once-only-order-placed")
record OnceOnlyOrderPlaced(String orderId) {}

@Reactor
class OnceOnlyOrderReactor {
    @OnceOnly
    void sendNotification(OnceOnlyOrderPlaced event) {
        // Runs once when the event is first observed, and is skipped during replay.
    }
}
```
