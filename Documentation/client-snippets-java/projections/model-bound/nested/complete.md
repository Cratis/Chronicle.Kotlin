```java title="Complete nested object projection"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ClearWith;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.Nested;
import io.cratis.chronicle.projections.SetFrom;
import io.cratis.chronicle.readModels.ReadModel;

@EventType(id = "slice-created-for-nested-complete")
record SliceCreatedForNestedComplete(String name) {}

@EventType(id = "command-set-for-nested-complete")
record CommandSetForNestedComplete(
    String commandId,
    String name,
    String schema,
    String rules,
    String stateSchema
) {}

@EventType(id = "command-renamed-for-nested-complete")
record CommandRenamedForNestedComplete(String commandId, String name) {}

@EventType(id = "command-definition-updated-for-nested-complete")
record CommandDefinitionUpdatedForNestedComplete(
    String commandId,
    String schema,
    String rules,
    String stateSchema
) {}

@EventType(id = "command-cleared-for-nested-complete")
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
