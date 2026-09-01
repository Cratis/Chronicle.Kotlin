```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.SetFrom;

@EventType
record IndexExplicitAccountOpened(String name, double initialBalance) {}

class IndexExplicitMbAccountInfo {
    @SetFrom(propertyPath = "name", eventType = IndexExplicitAccountOpened.class)
    public String name = "";

    @SetFrom(propertyPath = "initialBalance", eventType = IndexExplicitAccountOpened.class)
    public double balance = 0;
}
```
