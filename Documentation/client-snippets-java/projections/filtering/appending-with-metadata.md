```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.BlockingEventStore;

import java.util.UUID;

class FilteringAppendService {
    private final BlockingEventStore eventStore;

    FilteringAppendService(IEventStore eventStore) {
        this.eventStore = new BlockingEventStore(eventStore);
    }

    void appendOrders(String customerId) {
        // Appends to all observers — no extra metadata
        eventStore.getEventLog().append(
            UUID.randomUUID().toString(),
            new FilteringWithReactorOrderPlaced(customerId, 42.0));

        // Appends to all observers; additionally dispatched to observers filtering on "premium"
        var premiumOptions = new AppendOptionsBuilder().tag("premium").build();
        eventStore.getEventLog().append(
            UUID.randomUUID().toString(),
            new FilteringWithReactorOrderPlaced(customerId, 299.0),
            premiumOptions);

        // Appends with stream type; dispatched to observers filtering on "wholesale" stream type
        var wholesaleOptions = new AppendOptionsBuilder().eventStreamType("wholesale").build();
        eventStore.getEventLog().append(
            UUID.randomUUID().toString(),
            new FilteringWithReactorOrderPlaced(customerId, 1500.0),
            wholesaleOptions);
    }
}
```
