```java title="Update a nested object from multiple events"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ClearWith;
import io.cratis.chronicle.projections.FromEvent;

@EventType(id = "command-set-for-nested-multiple-from")
record CommandSetForNestedMultipleFrom(String name, String schema) {}

@EventType(id = "command-renamed-for-nested-multiple-from")
record CommandRenamedForNestedMultipleFrom(String name) {}

@EventType(id = "command-schema-updated-for-nested-multiple-from")
record CommandSchemaUpdatedForNestedMultipleFrom(String schema) {}

@EventType(id = "command-cleared-for-nested-multiple-from")
record CommandClearedForNestedMultipleFrom() {}

@FromEvent(eventType = CommandSetForNestedMultipleFrom.class)
@FromEvent(eventType = CommandRenamedForNestedMultipleFrom.class)
@FromEvent(eventType = CommandSchemaUpdatedForNestedMultipleFrom.class)
@ClearWith(eventType = CommandClearedForNestedMultipleFrom.class)
record CommandItemNestedMultipleFrom(String name, String schema) {}
```
