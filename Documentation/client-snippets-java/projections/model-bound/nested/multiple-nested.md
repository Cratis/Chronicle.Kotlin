```java title="Multiple nested objects on one parent"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ClearWith;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Nested;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record SliceCreatedForNestedMultiple(String name) {}

@EventType
record CommandSetForNestedMultiple(String name, String schema) {}

@EventType
record CommandClearedForNestedMultiple() {}

@EventType
record ValidationConfiguredForNestedMultiple(String rules, boolean isStrict) {}

@EventType
record ValidationRemovedForNestedMultiple() {}

@ReadModel
@FromEvent(eventType = SliceCreatedForNestedMultiple.class)
record SliceWithMultipleNestedObjects(
    String name,

    @Nested
    CommandItemNestedMultiple command,

    @Nested
    ValidationConfigNestedMultiple validation
) {}

@FromEvent(eventType = CommandSetForNestedMultiple.class)
@ClearWith(eventType = CommandClearedForNestedMultiple.class)
record CommandItemNestedMultiple(String name, String schema) {}

@FromEvent(eventType = ValidationConfiguredForNestedMultiple.class)
@ClearWith(eventType = ValidationRemovedForNestedMultiple.class)
record ValidationConfigNestedMultiple(String rules, boolean isStrict) {}
```
