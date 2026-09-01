```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Join;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "mb-joins-multiple-order-placed")
record MbJoinsMultipleOrderPlaced(String customerId) {}

@EventType(id = "mb-joins-multiple-customer-created")
record MbJoinsMultipleCustomerCreated(String name) {}

@EventType(id = "mb-joins-customer-updated")
record MbJoinsCustomerUpdated(String email) {}

@EventType(id = "mb-joins-shipping-address-set")
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
