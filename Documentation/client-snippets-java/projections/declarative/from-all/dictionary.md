```java title="Declarative FromAll with a dynamic dictionary key"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

import java.util.HashMap;
import java.util.Map;

@EventType
record UserRegisteredForEventCounts(String name) {}

@EventType
record OrderPlacedForEventCounts(String orderId) {}

class EventTypeCountsReadModel {
    public String id = "";
    public Map<String, Long> eventCountByType = new HashMap<>();
}

class EventTypeCountsProjection implements IProjectionFor<EventTypeCountsReadModel> {
    @Override
    public void define(IProjectionBuilderFor<EventTypeCountsReadModel> builder) {
        builder
            .fromAll(feb -> {
                feb.count("eventCountByType", "eventType.id");
            });
    }
}
```
