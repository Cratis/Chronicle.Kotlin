```java title="Nested object events"
import io.cratis.chronicle.events.EventType;

@EventType(id = "slice-created-for-nested-events")
record SliceCreatedForNestedEvents(String name) {}

@EventType(id = "command-set-for-nested-events")
record CommandSetForNestedEvents(String name, String schema) {}

@EventType(id = "command-cleared-for-nested-events")
record CommandClearedForNestedEvents() {}
```
