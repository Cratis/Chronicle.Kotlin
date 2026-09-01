```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;

@EventType
record ReactorsEventSequenceOrderShipped(String orderId) {}

@Reactor(id = "shipping-notifications", eventSequence = "outbox")
class ReactorsEventSequenceShippingNotifications {
    void shipped(ReactorsEventSequenceOrderShipped event) {
        // Observed from the "outbox" sequence rather than the default event log.
    }
}
```
