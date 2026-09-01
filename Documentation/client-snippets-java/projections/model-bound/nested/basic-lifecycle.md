```java title="Nested object lifecycle"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ClearWith;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Nested;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record CommandSetForNestedBasic(String name, String schema) {}

@EventType
record CommandClearedForNestedBasic() {}

@ReadModel
@FromEvent(eventType = CommandSetForNestedBasic.class)
record SliceWithNestedCommandBasic(
    String name,

    @Nested
    CommandItemNestedBasic command
) {}

@FromEvent(eventType = CommandSetForNestedBasic.class)
@ClearWith(eventType = CommandClearedForNestedBasic.class)
record CommandItemNestedBasic(String name, String schema) {}
```
