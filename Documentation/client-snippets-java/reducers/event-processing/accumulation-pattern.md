```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record EventProcessingMetricRecorded(double value) {}

@ReadModel
record EventProcessingStatistics(double sum, int count, double average) {
    EventProcessingStatistics() {
        this(0.0, 0, 0.0);
    }
}

@Reducer
class EventProcessingStatisticsReducer {
    EventProcessingStatistics recorded(EventProcessingMetricRecorded event, EventProcessingStatistics current) {
        double sum = (current == null ? 0.0 : current.sum()) + event.value();
        int count = (current == null ? 0 : current.count()) + 1;
        return new EventProcessingStatistics(sum, count, sum / count);
    }
}
```
