```java title="Clear a nested object"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ClearWith;
import io.cratis.chronicle.projections.FromEvent;

@EventType
record CommandSetForNestedClear(String name, String schema) {}

@EventType
record CommandClearedForNestedClear() {}

@FromEvent(eventType = CommandSetForNestedClear.class)
@ClearWith(eventType = CommandClearedForNestedClear.class)
record CommandItemNestedClear(String name, String schema) {}
```
