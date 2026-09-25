```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record EventProcessingOrderPlaced(String orderId) {}

@ReadModel
record EventProcessingOrder(String id, double total) {
    EventProcessingOrder() {
        this("", 0.0);
    }
}

interface EventProcessingShippingService {
    void schedule(EventProcessingOrder order);
}

@Reactor
class EventProcessingOrderProcessor {
    private final EventProcessingShippingService shipping;

    EventProcessingOrderProcessor(EventProcessingShippingService shipping) {
        this.shipping = shipping;
    }

    // `order` is looked up by the triggering event's event source id; a materialized read model
    // can lag this event or be null.
    void orderPlaced(EventProcessingOrderPlaced event, EventProcessingOrder order, EventContext context) {
        if (order != null) {
            shipping.schedule(order);
        }
    }
}
```
