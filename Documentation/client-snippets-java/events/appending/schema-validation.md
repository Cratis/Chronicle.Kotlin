```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingEventStore;

@EventType(id = "SchemaValidatedOrderPlaced")
record SchemaValidatedOrderPlaced(String customerId, double total) {}

class SchemaValidationExample {
    void append(IEventStore store, String eventSourceId, String customerId, double total) {
        var result = new BlockingEventStore(store).getEventLog().append(
            eventSourceId,
            new SchemaValidatedOrderPlaced(customerId, total));

        if (!result.isSuccess()) {
            result.getErrors().forEach(error ->
                System.out.println("Schema error: " + error));
        }
    }
}
```
