```java
import io.cratis.chronicle.IEventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.BlockingEventStore;

@EventType
record SchemaValidatedOrderPlaced(String customerId, double total) {}

class SchemaValidationExample {
    void append(IEventStore store, String eventSourceId, String customerId, double total) {
        var result = new BlockingEventStore(store).getEventLog().append(
            eventSourceId,
            new SchemaValidatedOrderPlaced(customerId, total));

        if (!result.isSuccess()) {
            result.getConstraintViolations().stream()
                .filter(violation -> "SchemaValidation".equals(violation.getConstraintId()))
                .forEach(violation ->
                    System.out.println("Schema error at " + violation.getDetails().get("path") +
                        ": " + violation.getMessage()));
        }
    }
}
```
