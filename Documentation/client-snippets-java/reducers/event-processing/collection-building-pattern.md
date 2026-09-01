```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.Reducer;
import io.cratis.chronicle.readModels.ReadModel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@EventType(id = "event-processing-customer-action")
record EventProcessingCustomerAction(String type, String description) {}

record EventProcessingActivity(String type, Instant timestamp, String description) {}

@ReadModel
record EventProcessingCustomerActivityLog(List<EventProcessingActivity> activities) {
    EventProcessingCustomerActivityLog() {
        this(List.of());
    }
}

@Reducer
class EventProcessingCustomerActivityLogReducer {
    EventProcessingCustomerActivityLog recorded(
            EventProcessingCustomerAction event,
            EventProcessingCustomerActivityLog current,
            EventContext context) {
        // Copy rather than mutate - current.activities() may still be referenced by a held snapshot
        List<EventProcessingActivity> activities = new ArrayList<>(current == null ? List.of() : current.activities());
        activities.add(new EventProcessingActivity(event.type(), context.getOccurred(), event.description()));

        return new EventProcessingCustomerActivityLog(activities);
    }
}
```
