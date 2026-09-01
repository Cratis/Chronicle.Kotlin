```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType(id = "reducers-filtering-order-placed")
record ReducersFilteringOrderPlaced(double totalAmount) {}

class ReducersFilteringMetadataExampleService {
    private final IEventLog eventLog;

    ReducersFilteringMetadataExampleService(IEventLog eventLog) {
        this.eventLog = eventLog;
    }

    void placeOrder(String eventSourceId, double totalAmount) {
        EventLogJavaBridge.append(
            eventLog,
            eventSourceId,
            new ReducersFilteringOrderPlaced(totalAmount),
            new AppendOptionsBuilder()
                .tag("priority")
                .eventSourceType("order")
                .eventStreamType("fulfillment")
                .build());
    }
}
```
