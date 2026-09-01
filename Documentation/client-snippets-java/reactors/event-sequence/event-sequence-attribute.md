```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.EventSequence;
import io.cratis.chronicle.observation.Reactor;

@EventType
record ReactorsEventSequenceOrderDelivered(String orderId) {}

@Reactor
@EventSequence("outbox")
class ReactorsEventSequenceDeliveryNotifications {
    void delivered(ReactorsEventSequenceOrderDelivered event) {
        // Observed from the "outbox" sequence, without giving up the conventional identifier.
    }
}
```
