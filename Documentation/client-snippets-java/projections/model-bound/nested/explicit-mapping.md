```java title="Explicit mappings on a nested type"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.ClearWith;
import io.cratis.chronicle.projections.FromEvent;
import io.cratis.chronicle.projections.SetFrom;

@EventType
record CommandSetForNestedExplicit(String commandName, String jsonSchema) {}

@EventType
record CommandSchemaUpdatedForNestedExplicit(String updatedSchema) {}

@EventType
record CommandClearedForNestedExplicit() {}

@FromEvent(eventType = CommandSetForNestedExplicit.class)
@FromEvent(eventType = CommandSchemaUpdatedForNestedExplicit.class)
@ClearWith(eventType = CommandClearedForNestedExplicit.class)
record CommandItemNestedExplicit(
    @SetFrom(propertyPath = "commandName", eventType = CommandSetForNestedExplicit.class)
    String name,

    @SetFrom(propertyPath = "jsonSchema", eventType = CommandSetForNestedExplicit.class)
    @SetFrom(propertyPath = "updatedSchema", eventType = CommandSchemaUpdatedForNestedExplicit.class)
    String schema
) {}
```
