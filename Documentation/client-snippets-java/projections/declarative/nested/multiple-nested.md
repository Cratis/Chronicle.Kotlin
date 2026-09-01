```java title="Multiple nested objects"
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.projections.IProjectionBuilderFor;
import io.cratis.chronicle.projections.IProjectionFor;

@EventType
record SliceCreatedWithMultipleNested(String name) {}

@EventType
record CommandSetWithMultipleNested(String name, String schema) {}

@EventType
record CommandClearedWithMultipleNested() {}

@EventType
record ValidationConfiguredWithMultipleNested(String ruleName) {}

@EventType
record ValidationRemovedWithMultipleNested() {}

record SliceWithMultipleNested(
    String name,
    CommandItemWithMultipleNested command,
    ValidationConfigWithMultipleNested validation) {}

record CommandItemWithMultipleNested(String name, String schema) {}

record ValidationConfigWithMultipleNested(String ruleName) {}

class SliceProjectionWithMultipleNested implements IProjectionFor<SliceWithMultipleNested> {
    @Override
    public void define(IProjectionBuilderFor<SliceWithMultipleNested> builder) {
        builder
            .from(SliceCreatedWithMultipleNested.class)
            .nested("command", CommandItemWithMultipleNested.class, nested -> {
                nested
                    .from(CommandSetWithMultipleNested.class)
                    .clearWith(CommandClearedWithMultipleNested.class);
                return null; // Java lambda returning Unit
            })
            .nested("validation", ValidationConfigWithMultipleNested.class, nested -> {
                nested
                    .from(ValidationConfiguredWithMultipleNested.class)
                    .clearWith(ValidationRemovedWithMultipleNested.class);
                return null; // Java lambda returning Unit
            });
    }
}
```
