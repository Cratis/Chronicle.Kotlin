```java
import io.cratis.chronicle.constraints.Constraint;
import io.cratis.chronicle.constraints.IConstraint;
import io.cratis.chronicle.constraints.IConstraintBuilder;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.UniqueConstraintBuilderJavaBridge;

@EventType
record ConstraintsUniqueUserRegistered(String email) {}

@EventType
record ConstraintsUniqueUserEmailChanged(String newEmail) {}

// The constraint is named through @Constraint(id = ...); the fluent unique builder has no
// withName() or removedWith() on the JVM.
@Constraint(id = "UniqueEmail")
class ConstraintsUniqueEmailAcrossEvents implements IConstraint {
    @Override
    public void define(IConstraintBuilder builder) {
        builder.unique(unique -> {
            UniqueConstraintBuilderJavaBridge.on(unique, ConstraintsUniqueUserRegistered.class, "email");
            UniqueConstraintBuilderJavaBridge.on(unique, ConstraintsUniqueUserEmailChanged.class, "newEmail");
        });
    }
}
```
