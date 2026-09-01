```java
import io.cratis.chronicle.constraints.IConstraint;
import io.cratis.chronicle.constraints.IConstraintBuilder;
import io.cratis.chronicle.events.EventType;

@EventType
class ConstraintsUniqueEventTypeMessageProjectInitialized {
}

class ConstraintsUniqueEventTypeMessageProjectInitialization implements IConstraint {
    @Override
    public void define(IConstraintBuilder builder) {
        builder.uniqueFor(ConstraintsUniqueEventTypeMessageProjectInitialized.class, "A project can only be initialized once.");
    }
}
```
