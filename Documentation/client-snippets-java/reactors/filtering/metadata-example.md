```java
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.IEventLog;
import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType(id = "reactors-filtering-order-placed")
record ReactorsFilteringOrderPlaced(double totalAmount) {}

class ReactorsFilteringMetadataExampleService {
    private final IEventLog eventLog;

    ReactorsFilteringMetadataExampleService(IEventLog eventLog) {
        this.eventLog = eventLog;
    }

    void placeOrder(String eventSourceId, double totalAmount) {
        EventLogJavaBridge.append(
            eventLog,
            eventSourceId,
            new ReactorsFilteringOrderPlaced(totalAmount),
            new AppendOptionsBuilder()
                .tag("priority")
                .eventSourceType("order")
                .eventStreamType("fulfillment")
                .build());
    }
}
```
