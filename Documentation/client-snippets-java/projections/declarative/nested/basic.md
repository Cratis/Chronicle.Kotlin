```java title="Nested object projection"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType(id = "slice-created-for-nested-basic")
record SliceCreatedForNestedBasic(String name) {}

@EventType(id = "command-set-for-declarative-nested-basic")
record CommandSetForDeclarativeNestedBasic(String name, String schema) {}

@EventType(id = "command-cleared-for-declarative-nested-basic")
record CommandClearedForDeclarativeNestedBasic() {}

record SliceForNestedBasic(String name, CommandItemForNestedBasic command) {}

record CommandItemForNestedBasic(String name, String schema) {}

class SliceProjectionForNestedBasic implements IProjectionFor<SliceForNestedBasic> {
    @Override
    public void define(IProjectionBuilderFor<SliceForNestedBasic> builder) {
        builder
            .from(SliceCreatedForNestedBasic.class)
            .nested("command", CommandItemForNestedBasic.class, nested -> {
                nested
                    .from(CommandSetForDeclarativeNestedBasic.class)
                    .clearWith(CommandClearedForDeclarativeNestedBasic.class);
                return null; // Java lambda returning Unit
            });
    }
}
```
