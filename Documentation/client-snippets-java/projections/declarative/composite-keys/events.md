```java title="Events used by composite key projections"
import io.cratis.chronicle.events.EventType;

@EventType
record CompositeOrderCreated(
    String customerId,
    String orderNumber,
    String customerName,
    String orderDate) {}

@EventType
record CompositeOrderShipped(
    String customerId,
    String orderNumber,
    String shippedDate) {}

@EventType
record CompositeUserAction(
    String userId,
    String action,
    String details) {}
```
