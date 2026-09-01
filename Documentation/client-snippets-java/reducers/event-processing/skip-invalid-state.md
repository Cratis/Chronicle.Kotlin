```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "event-processing-skip-item-added")
record EventProcessingSkipItemAdded(double price) {}

@ReadModel
record EventProcessingSkipOrderSummary(double total) {
    EventProcessingSkipOrderSummary() {
        this(0.0);
    }
}

@Reducer
class EventProcessingSkipOrderSummaryReducer {
    EventProcessingSkipOrderSummary itemAdded(EventProcessingSkipItemAdded event, EventProcessingSkipOrderSummary current, EventContext context) {
        // Can't add items if order doesn't exist
        if (current == null) return null;

        return new EventProcessingSkipOrderSummary(current.total() + event.price());
    }
}
```
