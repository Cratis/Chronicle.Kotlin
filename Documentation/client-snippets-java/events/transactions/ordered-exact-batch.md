```java
import io.cratis.chronicle.OperationContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingEventSequence;

final class OrderedCustomerUpdates {
    @EventType
    record CustomerUpdated(String value) {}

    static void commit(BlockingEventSequence eventLog, OperationContext context) {
        try (var transaction = eventLog.beginUnitOfWork(context)) {
            transaction.append("customer-1", new CustomerUpdated("first"));
            transaction.append("customer-2", new CustomerUpdated("second"));
            transaction.append("customer-1", new CustomerUpdated("third"));
            transaction.commit(); // one appendMany RPC, in exactly this order
        }
    }
}
```
