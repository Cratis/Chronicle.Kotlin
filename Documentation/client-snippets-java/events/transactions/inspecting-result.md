```java
import io.cratis.chronicle.OperationContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingEventStore;

@EventType
record InspectingResultOrderPlaced(String orderId, double totalAmount) {}

class EventsTransactionsInspectingResult {
    void commitAndInspect(BlockingEventStore store, String orderId, OperationContext context) {
        try (var transaction = store.getEventLog().beginUnitOfWork(context)) {
            transaction.append(orderId, new InspectingResultOrderPlaced(orderId, 42.0));
            transaction.commit();
            System.out.println("success=" + transaction.isSuccess());
        }
    }
}
```
