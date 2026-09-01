```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Increment;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
class MbCountersUserLoggedIn {}

@ReadModel
@FromEvent(eventType = MbCountersUserLoggedIn.class)
class MbCountersUserStatistics {
    @Increment(eventType = MbCountersUserLoggedIn.class)
    public int loginCount = 0;
}
```
