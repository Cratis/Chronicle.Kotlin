```java
// Requires io.cratis:chronicle 6.5.0 or later.
import io.cratis.chronicle.constraints.Constraint;
import io.cratis.chronicle.constraints.IConstraint;
import io.cratis.chronicle.constraints.IConstraintBuilder;
import io.cratis.chronicle.events.EventType;

@EventType
class ConstraintsUniqueEventTypeMessageProjectInitialized {
}

@Constraint
class ConstraintsUniqueEventTypeMessageProjectInitialization implements IConstraint {
    @Override
    public void define(IConstraintBuilder builder) {
        builder.uniqueFor(ConstraintsUniqueEventTypeMessageProjectInitialized.class, "A project can only be initialized once.");
    }
}
```
