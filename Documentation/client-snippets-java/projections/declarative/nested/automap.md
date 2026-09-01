```java title="AutoMap in a nested scope"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType(id = "slice-created-for-nested-automap")
record SliceCreatedForNestedAutoMap(String name) {}

@EventType(id = "command-set-for-nested-automap")
record CommandSetForNestedAutoMap(String name, String schema) {}

@EventType(id = "command-updated-for-nested-automap")
record CommandUpdatedForNestedAutoMap(String schema) {}

@EventType(id = "command-cleared-for-nested-automap")
record CommandClearedForNestedAutoMap() {}

record SliceForNestedAutoMap(String name, CommandItemForNestedAutoMap command) {}

record CommandItemForNestedAutoMap(String name, String schema) {}

class SliceProjectionForNestedAutoMap implements IProjectionFor<SliceForNestedAutoMap> {
    @Override
    public void define(IProjectionBuilderFor<SliceForNestedAutoMap> builder) {
        builder
            .from(SliceCreatedForNestedAutoMap.class)
            .nested("command", CommandItemForNestedAutoMap.class, nested -> {
                nested
                    .from(CommandSetForNestedAutoMap.class)
                    .from(CommandUpdatedForNestedAutoMap.class)
                    .clearWith(CommandClearedForNestedAutoMap.class);
                return null; // Java lambda returning Unit
            });
    }
}
```
