```java
// Requires io.cratis:chronicle 6.5.0 or later.
import io.cratis.chronicle.constraints.Constraint;
import io.cratis.chronicle.constraints.IConstraint;
import io.cratis.chronicle.constraints.IConstraintBuilder;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.UniqueConstraintBuilderJavaBridge;

@EventType
record ConstraintsUniqueMessageProjectCreated(String name) {}

@Constraint
class ConstraintsUniqueMessageProjectName implements IConstraint {
    @Override
    public void define(IConstraintBuilder builder) {
        builder.unique(unique -> {
            UniqueConstraintBuilderJavaBridge.on(unique, ConstraintsUniqueMessageProjectCreated.class, "name")
                .withMessage("A project with this name already exists.");
        });
    }
}
```
