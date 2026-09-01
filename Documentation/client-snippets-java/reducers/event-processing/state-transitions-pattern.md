```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import java.time.Instant;
import java.util.UUID;

@EventType(id = "event-processing-order-created-for-status")
record EventProcessingOrderCreatedForStatus(UUID orderId) {}

@EventType(id = "event-processing-order-paid")
record EventProcessingOrderPaid(UUID orderId) {}

@EventType(id = "event-processing-order-shipped")
record EventProcessingOrderShipped(UUID orderId) {}

@EventType(id = "event-processing-order-delivered")
record EventProcessingOrderDelivered(UUID orderId) {}

@EventType(id = "event-processing-order-cancelled")
record EventProcessingOrderCancelled(UUID orderId) {}

@ReadModel
record EventProcessingOrderStatus(String state, Instant lastUpdated) {
    EventProcessingOrderStatus() {
        this("", Instant.EPOCH);
    }
}

@Reducer
class EventProcessingOrderStatusReducer {
    EventProcessingOrderStatus created(EventProcessingOrderCreatedForStatus event, EventProcessingOrderStatus current, EventContext context) {
        return new EventProcessingOrderStatus("Created", context.getOccurred());
    }

    EventProcessingOrderStatus paid(EventProcessingOrderPaid event, EventProcessingOrderStatus current, EventContext context) {
        return new EventProcessingOrderStatus("Paid", context.getOccurred());
    }

    EventProcessingOrderStatus shipped(EventProcessingOrderShipped event, EventProcessingOrderStatus current, EventContext context) {
        return new EventProcessingOrderStatus("Shipped", context.getOccurred());
    }

    EventProcessingOrderStatus delivered(EventProcessingOrderDelivered event, EventProcessingOrderStatus current, EventContext context) {
        return new EventProcessingOrderStatus("Delivered", context.getOccurred());
    }

    EventProcessingOrderStatus cancelled(EventProcessingOrderCancelled event, EventProcessingOrderStatus current, EventContext context) {
        return new EventProcessingOrderStatus("Cancelled", context.getOccurred());
    }
}
```
