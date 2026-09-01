```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.Decrement;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Increment;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
class MbCountersUserConnected {}

@EventType
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
