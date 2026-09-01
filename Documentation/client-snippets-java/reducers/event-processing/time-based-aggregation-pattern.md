```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@EventType
record EventProcessingHourlyMetricRecorded(double value) {}

@ReadModel
record EventProcessingHourlyMetrics(Map<Integer, Double> metricsByHour) {
    EventProcessingHourlyMetrics() {
        this(Map.of());
    }
}

@Reducer
class EventProcessingHourlyMetricsReducer {
    EventProcessingHourlyMetrics recorded(EventProcessingHourlyMetricRecorded event, EventProcessingHourlyMetrics current, EventContext context) {
        Map<Integer, Double> metricsByHour = new HashMap<>(current == null ? Map.of() : current.metricsByHour());
        int hour = context.getOccurred().atZone(ZoneOffset.UTC).getHour();

        metricsByHour.merge(hour, event.value(), Double::sum);

        return new EventProcessingHourlyMetrics(metricsByHour);
    }
}
```
