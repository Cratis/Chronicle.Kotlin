```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.BlockingEventStore;

record TransactionalOrderPlaced(String orderId, double totalAmount) {}
record TransactionalInventoryReserved(String sku, int quantity) {}

class TransactionalOrderWorkflow {
    void commitOrder(IEventStore store) {
        var eventStore = new BlockingEventStore(store);

        // Rolls back on the way out unless it was committed, so a throw needs no catch.
        try (var unitOfWork = eventStore.beginUnitOfWork()) {
            eventStore.getTransactional().append(
                "order-123",
                new TransactionalOrderPlaced("order-123", 99.95));

            eventStore.getTransactional().append(
                "inventory-widget",
                new TransactionalInventoryReserved("widget", 1));

            unitOfWork.commit();
        }
    }
}
```
