```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "index-account-opened")
record IndexAccountOpened(String name, double initialBalance) {}

@ReadModel
@FromEvent(eventType = IndexAccountOpened.class)
class IndexAccountInfo {
    public String name = "";

    @SetFrom(propertyPath = "initialBalance")
    public double balance = 0;
}
```
