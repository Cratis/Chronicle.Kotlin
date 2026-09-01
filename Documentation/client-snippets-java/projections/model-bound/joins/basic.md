```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Join;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "mb-joins-order-placed")
record MbJoinsOrderPlaced(String customerId, double amount) {}

@EventType(id = "mb-joins-customer-created")
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
