```java
import io.cratis.chronicle.constraints.IConstraint;
import io.cratis.chronicle.constraints.IConstraintBuilder;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.UniqueConstraintBuilderJavaBridge;

@EventType(id = "constraints-unique-basic-project-created")
record ConstraintsUniqueBasicProjectCreated(String name) {
}

class ConstraintsUniqueBasicProjectName implements IConstraint {
    @Override
    public void define(IConstraintBuilder builder) {
        builder.unique(unique -> {
            UniqueConstraintBuilderJavaBridge.on(unique, ConstraintsUniqueBasicProjectCreated.class, ConstraintsUniqueBasicProjectCreated::name);
            return null;
        });
    }
}
```
