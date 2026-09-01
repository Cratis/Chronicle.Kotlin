```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.Decrement;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Increment;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "mb-counters-user-connected")
class MbCountersUserConnected {}

@EventType(id = "mb-counters-user-disconnected")
class MbCountersUserDisconnected {}

@ReadModel
@FromEvent(eventType = MbCountersUserConnected.class)
@FromEvent(eventType = MbCountersUserDisconnected.class)
class MbCountersServerStatistics {
    @Increment(eventType = MbCountersUserConnected.class)
    @Decrement(eventType = MbCountersUserDisconnected.class)
    public int activeConnections = 0;
}
```
