```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "event-processing-order-placed")
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

    // `order` is resolved by Chronicle itself: any parameter whose type carries @ReadModel is
    // materialized on demand, keyed by the triggering event's EventSourceId - strongly consistent
    // as of this handler call. It is null until something has been projected for that key.
    void orderPlaced(EventProcessingOrderPlaced event, EventProcessingOrder order, EventContext context) {
        if (order != null) {
            shipping.schedule(order);
        }
    }
}
```
