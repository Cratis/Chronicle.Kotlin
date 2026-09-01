```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "event-processing-basic-sync-recorded")
record EventProcessingBasicSyncRecorded(double value) {}

@ReadModel
record EventProcessingBasicSyncStats(double total) {
    EventProcessingBasicSyncStats() {
        this(0.0);
    }
}

@Reducer
class EventProcessingBasicSyncStatsReducer {
    // Process event and return new state
    EventProcessingBasicSyncStats recorded(EventProcessingBasicSyncRecorded event, EventProcessingBasicSyncStats current) {
        double total = current == null ? 0.0 : current.total();
        return new EventProcessingBasicSyncStats(total + event.value());
    }
}
```
