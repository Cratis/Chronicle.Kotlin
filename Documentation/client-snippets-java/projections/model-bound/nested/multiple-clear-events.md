```java title="Clear a nested object from multiple events"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ClearWith;
import io.cratis.chronicle.projections.FromEvent;

@EventType(id = "command-set-for-nested-multiple-clear")
record CommandSetForNestedMultipleClear(String name, String schema) {}

@EventType(id = "command-cleared-for-nested-multiple-clear")
record CommandClearedForNestedMultipleClear() {}

@EventType(id = "slice-archived-for-nested-multiple-clear")
record SliceArchivedForNestedMultipleClear() {}

@FromEvent(eventType = CommandSetForNestedMultipleClear.class)
@ClearWith(eventType = CommandClearedForNestedMultipleClear.class)
@ClearWith(eventType = SliceArchivedForNestedMultipleClear.class)
record CommandItemNestedMultipleClear(String name, String schema) {}
```
