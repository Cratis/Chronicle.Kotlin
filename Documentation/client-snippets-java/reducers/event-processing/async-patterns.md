```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "event-processing-async-recorded")
record EventProcessingAsyncRecorded(double value) {}

@EventType(id = "event-processing-async-recorded-with-context")
record EventProcessingAsyncRecordedWithContext(double value) {}

@ReadModel
record EventProcessingAsyncStats(double total) {
    EventProcessingAsyncStats() {
        this(0.0);
    }
}

// Java reducer methods are never suspending - they run on the observation thread directly, so
// there is no separate "async" shape to opt into. Await I/O with whatever blocking or
// CompletableFuture-based client your dependency exposes.
@Reducer
class EventProcessingAsyncStatsReducer {
    EventProcessingAsyncStats recorded(EventProcessingAsyncRecorded event, EventProcessingAsyncStats current) {
        double total = current == null ? 0.0 : current.total();
        return new EventProcessingAsyncStats(total + event.value());
    }

    EventProcessingAsyncStats recordedWithContext(
            EventProcessingAsyncRecordedWithContext event,
            EventProcessingAsyncStats current,
            EventContext context) {
        double total = current == null ? 0.0 : current.total();
        return new EventProcessingAsyncStats(total + event.value());
    }
}
```
