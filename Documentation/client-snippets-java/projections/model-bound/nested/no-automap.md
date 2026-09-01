```java title="Disable AutoMap on a nested type"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ClearWith;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.NoAutoMap;
import io.cratis.chronicle.projections.SetFrom;

@EventType(id = "command-set-for-nested-no-auto-map")
record CommandSetForNestedNoAutoMap(String commandName, String schema) {}

@EventType(id = "command-cleared-for-nested-no-auto-map")
record CommandClearedForNestedNoAutoMap() {}

@FromEvent(eventType = CommandSetForNestedNoAutoMap.class)
@ClearWith(eventType = CommandClearedForNestedNoAutoMap.class)
@NoAutoMap
record CommandItemNestedNoAutoMap(
    @SetFrom(propertyPath = "commandName", eventType = CommandSetForNestedNoAutoMap.class)
    String name,

    @SetFrom(propertyPath = "schema", eventType = CommandSetForNestedNoAutoMap.class)
    String schema
) {}
```
