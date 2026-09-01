```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record DecNotRewindableApiRequestCompleted(String endpoint, int statusCode, long durationMilliseconds) {}

class DecNotRewindablePerformanceMetric {
    public String timestamp = "";
}

class DecNotRewindablePerformanceMetricProjection implements IProjectionFor<DecNotRewindablePerformanceMetric> {
    @Override
    public void define(IProjectionBuilderFor<DecNotRewindablePerformanceMetric> builder) {
        builder
            .notRewindable()
            .autoMap()
            .from(DecNotRewindableApiRequestCompleted.class, fb -> {
                fb.set("timestamp").toEventContextProperty("occurred");
                return null; // Java lambda returning Unit
            });
    }
}
```
