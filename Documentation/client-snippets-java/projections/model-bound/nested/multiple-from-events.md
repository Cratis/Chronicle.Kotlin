```java title="Update a nested object from multiple events"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ClearWith;
import io.cratis.chronicle.projections.FromEvent;

@EventType
record CommandSetForNestedMultipleFrom(String name, String schema) {}

@EventType
record CommandRenamedForNestedMultipleFrom(String name) {}

@EventType
record CommandSchemaUpdatedForNestedMultipleFrom(String schema) {}

@EventType
record CommandClearedForNestedMultipleFrom() {}

@FromEvent(eventType = CommandSetForNestedMultipleFrom.class)
@FromEvent(eventType = CommandRenamedForNestedMultipleFrom.class)
@FromEvent(eventType = CommandSchemaUpdatedForNestedMultipleFrom.class)
@ClearWith(eventType = CommandClearedForNestedMultipleFrom.class)
record CommandItemNestedMultipleFrom(String name, String schema) {}
```
