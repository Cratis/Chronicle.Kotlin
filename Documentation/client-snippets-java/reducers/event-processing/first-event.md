```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import java.time.Instant;
import java.util.UUID;

@EventType(id = "event-processing-order-created")
record EventProcessingOrderCreated(UUID orderId) {}

@EventType(id = "event-processing-item-added")
record EventProcessingItemAdded(double price) {}

@ReadModel
record EventProcessingOrderSummary(UUID orderId, double total, Instant lastUpdated) {
    EventProcessingOrderSummary() {
        this(new UUID(0, 0), 0.0, Instant.EPOCH);
    }
}

@Reducer
class EventProcessingOrderSummaryReducer {
    EventProcessingOrderSummary created(EventProcessingOrderCreated event, EventProcessingOrderSummary current, EventContext context) {
        return new EventProcessingOrderSummary(event.orderId(), 0.0, context.getOccurred());
    }

    EventProcessingOrderSummary itemAdded(EventProcessingItemAdded event, EventProcessingOrderSummary current, EventContext context) {
        if (current == null) return null; // Skip if no order exists

        return new EventProcessingOrderSummary(current.orderId(), current.total() + event.price(), context.getOccurred());
    }
}
```
