```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingEventStore;

@EventType
record TransactionalOrderPlaced(String orderId, double totalAmount) {}
@EventType
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

            // Consecutive events for the same event source and options append atomically;
            // the unit of work is not atomic across event sources.
            unitOfWork.commit();
            if (!unitOfWork.unwrap().isSuccess()) {
                throw new IllegalStateException("Commit failed: " + unitOfWork.unwrap().getConstraintViolations() +
                    unitOfWork.unwrap().getConcurrencyViolations() + unitOfWork.unwrap().getAppendErrors());
            }
        }
    }
}
```
