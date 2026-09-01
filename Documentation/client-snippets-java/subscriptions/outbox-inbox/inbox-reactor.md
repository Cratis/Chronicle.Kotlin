```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;

@EventType
record SubscriptionsOutboxInboxOrderPlaced(String orderId) {}

@Reactor
class SubscriptionsOutboxInboxIncomingOrdersReactor {
    void orderPlaced(SubscriptionsOutboxInboxOrderPlaced event, EventContext context) {
        // Handles OrderPlaced events from any subscribed source event store
        process(event.orderId());
    }

    private void process(String orderId) {}
}
```
