```java title="Multiple nested events"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType(id = "slice-created-for-nested-updates")
record SliceCreatedForNestedUpdates(String name) {}

@EventType(id = "command-set-for-nested-updates")
record CommandSetForNestedUpdates(String name, String schema) {}

@EventType(id = "command-renamed-for-nested-updates")
record CommandRenamedForNestedUpdates(String newName) {}

@EventType(id = "command-schema-updated-for-nested-updates")
record CommandSchemaUpdatedForNestedUpdates(String updatedSchema) {}

@EventType(id = "command-cleared-for-nested-updates")
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
                        return null; // Java lambda returning Unit
                    })
                    .from(CommandSchemaUpdatedForNestedUpdates.class, fb -> {
                        fb.<String>set("schema").to(e -> e.updatedSchema());
                        return null; // Java lambda returning Unit
                    })
                    .clearWith(CommandClearedForNestedUpdates.class);
                return null; // Java lambda returning Unit
            });
    }
}
```
