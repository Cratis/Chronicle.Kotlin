```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.BlockingEventStore;

@EventType
record TaggedOrderPlaced(String customerId, double total) {}

class TaggedCheckoutService {
    private final BlockingEventStore eventStore;

    TaggedCheckoutService(IEventStore eventStore) {
        this.eventStore = new BlockingEventStore(eventStore);
    }

    void placeOrder(String orderId, String customerId, double total) {
        var options = new AppendOptionsBuilder().tag("checkout").tag("priority").build();
        eventStore.getEventLog().append(orderId, new TaggedOrderPlaced(customerId, total), options);
    }
}
```
