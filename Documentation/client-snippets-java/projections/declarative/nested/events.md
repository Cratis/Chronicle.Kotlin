```java title="Nested object events"
import io.cratis.chronicle.events.EventType;

@EventType
record SliceCreatedForNestedEvents(String name) {}

@EventType
record CommandSetForNestedEvents(String name, String schema) {}

@EventType
record CommandClearedForNestedEvents() {}
```
