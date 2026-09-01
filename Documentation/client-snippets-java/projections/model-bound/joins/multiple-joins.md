```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Join;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record MbJoinsMultipleOrderPlaced(String customerId) {}

@EventType
record MbJoinsMultipleCustomerCreated(String name) {}

@EventType
record MbJoinsCustomerUpdated(String email) {}

@EventType
record MbJoinsShippingAddressSet(String address) {}

@ReadModel
@FromEvent(eventType = MbJoinsMultipleOrderPlaced.class)
class MbJoinsEnrichedOrder {
    @SetFrom(propertyPath = "customerId", eventType = MbJoinsMultipleOrderPlaced.class)
    public String customerId = "";

    @Join(eventType = MbJoinsMultipleCustomerCreated.class, on = "customerId")
    public String customerName = "";

    @Join(eventType = MbJoinsCustomerUpdated.class, on = "customerId")
    public String customerEmail = "";

    // ShippingAddressSet is raised on the order's own event source, so it joins on the
    // read model's own event source id rather than a separate correlating property.
    @Join(eventType = MbJoinsShippingAddressSet.class)
    public String shippingAddress = "";
}
```
