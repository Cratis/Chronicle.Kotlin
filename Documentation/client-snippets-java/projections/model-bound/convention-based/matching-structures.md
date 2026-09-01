```java title="Matching nested structures and collections"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.readModels.ReadModel;
import java.util.List;

record ConventionAddress(String street, String city, String postalCode) {}

record ConventionLineItem(String productName, double unitPrice, int quantity) {}

@EventType
record ConventionCustomerRegistered(
    String firstName,
    String lastName,
    ConventionAddress billingAddress,
    ConventionAddress shippingAddress) {}

@EventType
record ConventionOrderCreated(String customerEmail, List<ConventionLineItem> items, List<String> tags) {}

@ReadModel
@FromEvent(eventType = ConventionCustomerRegistered.class)
class ConventionCustomer {
    public String firstName = "";
    public String lastName = "";
    public ConventionAddress billingAddress;
    public ConventionAddress shippingAddress;
}

@ReadModel
@FromEvent(eventType = ConventionOrderCreated.class)
class ConventionOrder {
    public String customerEmail = "";
    public List<ConventionLineItem> items = List.of();
    public List<String> tags = List.of();
}
```
