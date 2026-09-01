```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Join;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record MbJoinsOrderPlaced(String customerId, double amount) {}

@EventType
record MbJoinsCustomerCreated(String name) {}

@ReadModel
@FromEvent(eventType = MbJoinsOrderPlaced.class)
class MbJoinsOrderSummary {
    @SetFrom(propertyPath = "amount", eventType = MbJoinsOrderPlaced.class)
    public double amount = 0.0;

    @SetFrom(propertyPath = "customerId", eventType = MbJoinsOrderPlaced.class)
    public String customerId = "";

    @Join(eventType = MbJoinsCustomerCreated.class, on = "customerId", eventPropertyName = "name")
    public String customerName = "";
}
```
