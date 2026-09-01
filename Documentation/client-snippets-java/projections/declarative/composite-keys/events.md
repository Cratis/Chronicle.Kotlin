```java title="Events used by composite key projections"
import io.cratis.chronicle.events.EventType;

@EventType(id = "composite-order-created")
record CompositeOrderCreated(
    String customerId,
    String orderNumber,
    String customerName,
    String orderDate) {}

@EventType(id = "composite-order-shipped")
record CompositeOrderShipped(
    String customerId,
    String orderNumber,
    String shippedDate) {}

@EventType(id = "composite-user-action")
record CompositeUserAction(
    String userId,
    String action,
    String details) {}
```
