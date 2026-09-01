```java title="Clear a nested object"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ClearWith;
import io.cratis.chronicle.projections.FromEvent;

@EventType(id = "command-set-for-nested-clear")
record CommandSetForNestedClear(String name, String schema) {}

@EventType(id = "command-cleared-for-nested-clear")
record CommandClearedForNestedClear() {}

@FromEvent(eventType = CommandSetForNestedClear.class)
@ClearWith(eventType = CommandClearedForNestedClear.class)
record CommandItemNestedClear(String name, String schema) {}
```
