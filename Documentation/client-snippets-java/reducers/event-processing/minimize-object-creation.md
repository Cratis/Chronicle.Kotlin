```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record EventProcessingMinimalMetricRecorded(double value) {}

@ReadModel
record EventProcessingMinimalStats(int count, double sum) {
    EventProcessingMinimalStats() {
        this(0, 0.0);
    }
}

@Reducer
class EventProcessingMinimalStatsReducer {
    // Efficient - only creates a new object when needed
    EventProcessingMinimalStats recorded(EventProcessingMinimalMetricRecorded event, EventProcessingMinimalStats current) {
        if (current == null) return new EventProcessingMinimalStats(1, event.value());

        return new EventProcessingMinimalStats(current.count() + 1, current.sum() + event.value());
    }
}
```
