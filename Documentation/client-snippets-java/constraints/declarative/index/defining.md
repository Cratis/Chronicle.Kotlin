```java
import io.cratis.chronicle.constraints.IConstraint;
import io.cratis.chronicle.constraints.IConstraintBuilder;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.UniqueConstraintBuilderJavaBridge;

@EventType(id = "constraints-unique-defining-project-created")
record ConstraintsUniqueDefiningProjectCreated(String name) {
}

class ConstraintsUniqueDefiningProjectName implements IConstraint {
    @Override
    public void define(IConstraintBuilder builder) {
        builder.unique(unique -> {
            UniqueConstraintBuilderJavaBridge.on(unique, ConstraintsUniqueDefiningProjectCreated.class, ConstraintsUniqueDefiningProjectCreated::name);
            return null;
        });
    }
}
```
