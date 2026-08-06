```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;

@EventType(id = "reactors-event-sequence-order-shipped")
record ReactorsEventSequenceOrderShipped(String orderId) {}

@Reactor(id = "shipping-notifications", eventSequence = "outbox")
class ReactorsEventSequenceShippingNotifications {
    void shipped(ReactorsEventSequenceOrderShipped event) {
        // Observed from the "outbox" sequence rather than the default event log.
    }
}
```
