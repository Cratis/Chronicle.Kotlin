```java title="Complete nested object projection"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ClearWith;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Nested;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType
record SliceCreatedForNestedComplete(String name) {}

@EventType
record CommandSetForNestedComplete(
    String commandId,
    String name,
    String schema,
    String rules,
    String stateSchema
) {}

@EventType
record CommandRenamedForNestedComplete(String commandId, String name) {}

@EventType
record CommandDefinitionUpdatedForNestedComplete(
    String commandId,
    String schema,
    String rules,
    String stateSchema
) {}

@EventType
record CommandClearedForNestedComplete() {}

@ReadModel
@FromEvent(eventType = SliceCreatedForNestedComplete.class)
record SliceNestedComplete(
    String name,

    @Nested
    CommandItemNestedComplete command
) {}

@FromEvent(eventType = CommandSetForNestedComplete.class)
@FromEvent(eventType = CommandRenamedForNestedComplete.class)
@FromEvent(eventType = CommandDefinitionUpdatedForNestedComplete.class)
@ClearWith(eventType = CommandClearedForNestedComplete.class)
record CommandItemNestedComplete(
    @SetFrom(propertyPath = "commandId", eventType = CommandSetForNestedComplete.class)
    String id,

    @SetFrom(propertyPath = "name", eventType = CommandSetForNestedComplete.class)
    @SetFrom(propertyPath = "name", eventType = CommandRenamedForNestedComplete.class)
    String name,

    @SetFrom(propertyPath = "schema", eventType = CommandSetForNestedComplete.class)
    @SetFrom(propertyPath = "schema", eventType = CommandDefinitionUpdatedForNestedComplete.class)
    String schema,

    @SetFrom(propertyPath = "rules", eventType = CommandSetForNestedComplete.class)
    @SetFrom(propertyPath = "rules", eventType = CommandDefinitionUpdatedForNestedComplete.class)
    String rules,

    @SetFrom(propertyPath = "stateSchema", eventType = CommandSetForNestedComplete.class)
    @SetFrom(propertyPath = "stateSchema", eventType = CommandDefinitionUpdatedForNestedComplete.class)
    String stateSchema
) {}
```
