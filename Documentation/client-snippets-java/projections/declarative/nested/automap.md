```java title="AutoMap in a nested scope"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record SliceCreatedForNestedAutoMap(String name) {}

@EventType
record CommandSetForNestedAutoMap(String name, String schema) {}

@EventType
record CommandUpdatedForNestedAutoMap(String schema) {}

@EventType
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
            });
    }
}
```
