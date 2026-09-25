```java
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
        // Not sent to the kernel by io.cratis:chronicle 6.4.0; a violation carries the kernel's own message.
        builder.uniqueFor(ConstraintsUniqueEventTypeMessageProjectInitialized.class, "A project can only be initialized once.");
    }
}
```
