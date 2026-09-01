```java
import io.cratis.chronicle.events.EventType;

@EventType(id = "dec-from-event-sequence-order-created")
record DecFromEventSequenceOrderCreated(
    String orderNumber,
    String customerId,
    double totalAmount) {}

@EventType(id = "dec-from-event-sequence-order-updated")
record DecFromEventSequenceOrderUpdated(
    String orderNumber,
    double newTotalAmount) {}

@EventType(id = "dec-from-event-sequence-order-shipped")
record DecFromEventSequenceOrderShipped(
    String orderNumber,
    String shippedAt) {}
```
