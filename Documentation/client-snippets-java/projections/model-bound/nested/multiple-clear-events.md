```java title="Clear a nested object from multiple events"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ClearWith;
import io.cratis.chronicle.projections.FromEvent;

@EventType
record CommandSetForNestedMultipleClear(String name, String schema) {}

@EventType
record CommandClearedForNestedMultipleClear() {}

@EventType
record SliceArchivedForNestedMultipleClear() {}

@FromEvent(eventType = CommandSetForNestedMultipleClear.class)
@ClearWith(eventType = CommandClearedForNestedMultipleClear.class)
@ClearWith(eventType = SliceArchivedForNestedMultipleClear.class)
record CommandItemNestedMultipleClear(String name, String schema) {}
```
