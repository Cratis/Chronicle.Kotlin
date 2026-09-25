```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record IndexExplicitAccountOpened(String name, double initialBalance) {}

@ReadModel
@FromEvent(eventType = IndexExplicitAccountOpened.class)
class IndexExplicitMbAccountInfo {
    @SetFrom(propertyPath = "name", eventType = IndexExplicitAccountOpened.class)
    public String name = "";

    @SetFrom(propertyPath = "initialBalance", eventType = IndexExplicitAccountOpened.class)
    public double balance = 0;
}
```
