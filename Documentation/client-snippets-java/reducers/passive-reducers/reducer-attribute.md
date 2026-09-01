```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;
import java.time.Instant;

@EventType
record PassiveReducersDataRecorded(double value) {}

@ReadModel
record PassiveReducersAnalytics(int recordCount, double totalValue, Instant lastUpdated) {
    PassiveReducersAnalytics() {
        this(0, 0.0, Instant.EPOCH);
    }
}

@Reducer(isActive = false)
class PassiveReducersTemporaryAnalyticsReducer {
    PassiveReducersAnalytics recorded(
            PassiveReducersDataRecorded event,
            PassiveReducersAnalytics current,
            EventContext context) {
        int count = current == null ? 0 : current.recordCount();
        double sum = current == null ? 0.0 : current.totalValue();
        return new PassiveReducersAnalytics(count + 1, sum + event.value(), context.getOccurred());
    }
}
```
