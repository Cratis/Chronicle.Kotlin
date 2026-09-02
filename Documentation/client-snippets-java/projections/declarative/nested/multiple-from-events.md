```java title="Multiple nested events"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record SliceCreatedForNestedUpdates(String name) {}

@EventType
record CommandSetForNestedUpdates(String name, String schema) {}

@EventType
record CommandRenamedForNestedUpdates(String newName) {}

@EventType
record CommandSchemaUpdatedForNestedUpdates(String updatedSchema) {}

@EventType
record CommandClearedForNestedUpdates() {}

record SliceForNestedUpdates(String name, CommandItemForNestedUpdates command) {}

record CommandItemForNestedUpdates(String name, String schema) {}

class SliceProjectionForNestedUpdates implements IProjectionFor<SliceForNestedUpdates> {
    @Override
    public void define(IProjectionBuilderFor<SliceForNestedUpdates> builder) {
        builder
            .from(SliceCreatedForNestedUpdates.class)
            .nested("command", CommandItemForNestedUpdates.class, nested -> {
                nested
                    .from(CommandSetForNestedUpdates.class)
                    .from(CommandRenamedForNestedUpdates.class, fb -> {
                        fb.<String>set("name").to(e -> e.newName());
                    })
                    .from(CommandSchemaUpdatedForNestedUpdates.class, fb -> {
                        fb.<String>set("schema").to(e -> e.updatedSchema());
                    })
                    .clearWith(CommandClearedForNestedUpdates.class);
            });
    }
}
```
