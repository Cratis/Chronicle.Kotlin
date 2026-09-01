```java title="Multiple nested objects on one parent"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ClearWith;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Nested;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "slice-created-for-nested-multiple")
record SliceCreatedForNestedMultiple(String name) {}

@EventType(id = "command-set-for-nested-multiple")
record CommandSetForNestedMultiple(String name, String schema) {}

@EventType(id = "command-cleared-for-nested-multiple")
record CommandClearedForNestedMultiple() {}

@EventType(id = "validation-configured-for-nested-multiple")
record ValidationConfiguredForNestedMultiple(String rules, boolean isStrict) {}

@EventType(id = "validation-removed-for-nested-multiple")
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
