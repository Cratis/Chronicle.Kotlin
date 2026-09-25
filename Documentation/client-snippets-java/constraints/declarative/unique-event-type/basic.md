```java
import io.cratis.chronicle.constraints.Constraint;
import io.cratis.chronicle.constraints.IConstraint;
import io.cratis.chronicle.constraints.IConstraintBuilder;
import io.cratis.chronicle.events.EventType;

@EventType
class ConstraintsUniqueEventTypeProjectInitialized {
}

@Constraint
class ConstraintsUniqueEventTypeProjectInitialization implements IConstraint {
    @Override
    public void define(IConstraintBuilder builder) {
        builder.uniqueFor(ConstraintsUniqueEventTypeProjectInitialized.class, "");
    }
}
```
