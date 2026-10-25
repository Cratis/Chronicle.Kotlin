```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.concepts.EventSourceId;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingEventStore;
import java.util.UUID;

// A strongly typed domain concept, stored as a string on the wire.
record EventSourceIdCustomerId(String value) implements EventSourceId {
    static EventSourceIdCustomerId newId() {
        return new EventSourceIdCustomerId(UUID.randomUUID().toString());
    }
}

@EventType
record EventSourceIdOrderPlaced(EventSourceIdCustomerId customerId, double total) {}

class EventSourceIdOrderService {
    private final BlockingEventStore store;

    EventSourceIdOrderService(IEventStore store) {
        this.store = new BlockingEventStore(store);
    }

    void placeOrder(double total) {
        var customerId = EventSourceIdCustomerId.newId();
        store.getEventLog().append(customerId, new EventSourceIdOrderPlaced(customerId, total));
    }
}
```
