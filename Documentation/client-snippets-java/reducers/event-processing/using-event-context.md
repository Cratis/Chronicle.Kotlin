```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import java.time.Instant;
import java.util.UUID;

@EventType(id = "event-processing-context-order-placed")
record EventProcessingContextOrderPlaced(UUID orderId, double amount) {}

@ReadModel
record EventProcessingOrderSummaryWithContext(
    UUID orderId,
    double total,
    Instant placedAt,
    String placedBy,
    UUID correlationId) {
    EventProcessingOrderSummaryWithContext() {
        this(new UUID(0, 0), 0.0, Instant.EPOCH, "", new UUID(0, 0));
    }
}

@Reducer
class EventProcessingOrderSummaryWithContextReducer {
    EventProcessingOrderSummaryWithContext placed(EventProcessingContextOrderPlaced event, EventProcessingOrderSummaryWithContext current, EventContext context) {
        return new EventProcessingOrderSummaryWithContext(
            event.orderId(),
            event.amount(),
            context.getOccurred(),
            context.getCausedBy().getSubject(),
            context.getCorrelationId());
    }
}
```
