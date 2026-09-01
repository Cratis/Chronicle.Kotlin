```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.projections.SetValue;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record MbSetValueOrderPlaced(String customerName) {}

@EventType
record MbSetValueOrderCanceled() {}

@ReadModel
@FromEvent(eventType = MbSetValueOrderPlaced.class)
@FromEvent(eventType = MbSetValueOrderCanceled.class)
class MbSetValueOrder {
    @SetFrom(propertyPath = "customerName", eventType = MbSetValueOrderPlaced.class)
    public String customerName = "";

    @SetValue(eventType = MbSetValueOrderPlaced.class, value = "active")
    @SetValue(eventType = MbSetValueOrderCanceled.class, value = "canceled")
    public String status = "";
}
```
