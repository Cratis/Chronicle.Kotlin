```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reactor;

import java.util.List;

@EventType(id = "event-processing-signatures-order-placed")
record EventProcessingSignaturesOrderPlaced(String orderId) {}

@EventType(id = "event-processing-signatures-order-shipped")
record EventProcessingSignaturesOrderShipped(String orderId) {}

@EventType(id = "event-processing-signatures-order-cancelled")
record EventProcessingSignaturesOrderCancelled(String orderId) {}

@EventType(id = "event-processing-signatures-refund-issued")
record EventProcessingSignaturesRefundIssued(String orderId, double amount) {}

@EventType(id = "event-processing-signatures-order-archived")
record EventProcessingSignaturesOrderArchived(String orderId) {}

@Reactor
class EventProcessingSignaturesReactor {
    // (event) - no metadata needed, no side effect
    void placed(EventProcessingSignaturesOrderPlaced event) {
    }

    // (event, context) - no side effect
    void shipped(EventProcessingSignaturesOrderShipped event, EventContext context) {
    }

    // (event) - returns a single side-effect event
    EventProcessingSignaturesOrderArchived cancelled(EventProcessingSignaturesOrderCancelled event) {
        return new EventProcessingSignaturesOrderArchived(event.orderId());
    }

    // (event, context) - returns a list of side-effect events
    List<Object> refundIssued(EventProcessingSignaturesRefundIssued event, EventContext context) {
        return List.of(new EventProcessingSignaturesOrderArchived(event.orderId()));
    }
}
```
