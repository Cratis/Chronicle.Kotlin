```java title="Nested object projection"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record SliceCreatedForNestedBasic(String name) {}

@EventType
record CommandSetForDeclarativeNestedBasic(String name, String schema) {}

@EventType
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
