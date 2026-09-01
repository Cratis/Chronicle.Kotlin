```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import java.util.UUID;

@EventType
record EventProcessingMethodDiscoveryOrderCreated(UUID orderId) {}

@EventType
record EventProcessingMethodDiscoveryItemAdded(double price) {}

@ReadModel
record EventProcessingMethodDiscoveryOrderSummary(UUID orderId, double total) {
    EventProcessingMethodDiscoveryOrderSummary() {
        this(new UUID(0, 0), 0.0);
    }
}

@Reducer
class EventProcessingMethodDiscoveryOrderSummaryReducer {
    // Discovery matches on the first parameter's type, not the method name - a descriptive name
    // such as `created` for an OrderCreated event is good practice, not a requirement.
    EventProcessingMethodDiscoveryOrderSummary created(
            EventProcessingMethodDiscoveryOrderCreated event,
            EventProcessingMethodDiscoveryOrderSummary current,
            EventContext context) {
        return new EventProcessingMethodDiscoveryOrderSummary(event.orderId(), 0.0);
    }

    EventProcessingMethodDiscoveryOrderSummary itemAdded(
            EventProcessingMethodDiscoveryItemAdded event,
            EventProcessingMethodDiscoveryOrderSummary current,
            EventContext context) {
        if (current == null) return null;

        return new EventProcessingMethodDiscoveryOrderSummary(current.orderId(), current.total() + event.price());
    }
}
```
