```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record MbEventSeqLocalEvent(String data) {}

// No @EventSequence needed — an observer with no event sequence specified observes the event log.
@ReadModel
@FromEvent(eventType = MbEventSeqLocalEvent.class)
record MbEventSeqLocalSnapshot(
    @SetFrom(propertyPath = "data", eventType = MbEventSeqLocalEvent.class)
    String data
) {}
```
