```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.RemovedWith;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record MbRemovalAccountOpened(String name, double balance) {}

@EventType
class MbRemovalAccountClosed {}

@ReadModel
@FromEvent(eventType = MbRemovalAccountOpened.class)
@RemovedWith(eventType = MbRemovalAccountClosed.class)
class MbRemovalAccount {
    @SetFrom(propertyPath = "name", eventType = MbRemovalAccountOpened.class)
    public String name = "";

    @SetFrom(propertyPath = "balance", eventType = MbRemovalAccountOpened.class)
    public double balance = 0.0;
}
```
