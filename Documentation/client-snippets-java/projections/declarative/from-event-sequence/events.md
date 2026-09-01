```java
import io.cratis.chronicle.events.EventType;

@EventType
record DecFromEventSequenceOrderCreated(
    String orderNumber,
    String customerId,
    double totalAmount) {}

@EventType
record DecFromEventSequenceOrderUpdated(
    String orderNumber,
    double newTotalAmount) {}

@EventType
record DecFromEventSequenceOrderShipped(
    String orderNumber,
    String shippedAt) {}
```
