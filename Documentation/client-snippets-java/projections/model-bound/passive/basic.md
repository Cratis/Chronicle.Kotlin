```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.Passive;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record MbPassiveSnapshotCreated(String data) {}

@Passive
@ReadModel
@FromEvent(eventType = MbPassiveSnapshotCreated.class)
class MbPassiveHistoricalSnapshot {
    @SetFrom(propertyPath = "data", eventType = MbPassiveSnapshotCreated.class)
    public String data = "";
}
```
