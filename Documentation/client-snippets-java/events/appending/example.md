```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingEventStore;

@EventType
record OrderPlaced(String customerId, double total) {}

class CheckoutService {
    private final BlockingEventStore store;

    CheckoutService(IEventStore store) {
        this.store = new BlockingEventStore(store);
    }

    void placeOrder(String orderId, String customerId, double total) {
        var result = store.getEventLog().append(orderId, new OrderPlaced(customerId, total));

        if (!result.isSuccess()) {
            // Decide whether to retry or surface a conflict to the caller.
        }
    }
}
```
