```java
import io.cratis.chronicle.OperationContext;
import io.cratis.chronicle.java.BlockingEventStore;

record TransactionalOrderPlaced(String orderId, double totalAmount) {}
record TransactionalInventoryReserved(String sku, int quantity) {}

class TransactionalOrderWorkflow {
    void commitOrder(BlockingEventStore store, OperationContext context) {
        try (var transaction = store.getEventLog().beginUnitOfWork(context)) {
            transaction.append("order-123", new TransactionalOrderPlaced("order-123", 99.95));
            transaction.append("inventory-widget", new TransactionalInventoryReserved("widget", 1));
            transaction.commit();
        }
    }
}
```
