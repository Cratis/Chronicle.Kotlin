```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import java.time.Instant;

@EventType
record EventProcessingPatternWithContextRecorded(double value) {}

@ReadModel
record EventProcessingPatternWithContextStats(double total, Instant lastUpdated) {
    EventProcessingPatternWithContextStats() {
        this(0.0, Instant.EPOCH);
    }
}

@Reducer
class EventProcessingPatternWithContextStatsReducer {
    // Access occurred time, correlation id, etc. via the third parameter
    EventProcessingPatternWithContextStats recorded(
            EventProcessingPatternWithContextRecorded event,
            EventProcessingPatternWithContextStats current,
            EventContext context) {
        double total = current == null ? 0.0 : current.total();
        return new EventProcessingPatternWithContextStats(total + event.value(), context.getOccurred());
    }
}
```
