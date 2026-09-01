```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.Count;
import io.cratis.chronicle.projections.Decrement;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Increment;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record MbCountersItemCreated(String name, int initialQuantity) {}

@EventType
class MbCountersItemRestocked {}

@EventType
class MbCountersItemSold {}

@ReadModel
@FromEvent(eventType = MbCountersItemCreated.class)
@FromEvent(eventType = MbCountersItemRestocked.class)
@FromEvent(eventType = MbCountersItemSold.class)
class MbCountersInventoryItem {
    @SetFrom(propertyPath = "name", eventType = MbCountersItemCreated.class)
    public String name = "";

    @SetFrom(propertyPath = "initialQuantity", eventType = MbCountersItemCreated.class)
    @Increment(eventType = MbCountersItemRestocked.class)
    @Decrement(eventType = MbCountersItemSold.class)
    public int quantity = 0;

    @Count(eventType = MbCountersItemRestocked.class)
    public int restockCount = 0;

    @Count(eventType = MbCountersItemSold.class)
    public int salesCount = 0;
}
```
