```java
import io.cratis.chronicle.constraints.IConstraint;
import io.cratis.chronicle.constraints.IConstraintBuilder;
import io.cratis.chronicle.events.EventType;

@EventType(id = "constraints-unique-event-type-project-initialized")
class ConstraintsUniqueEventTypeProjectInitialized {
}

class ConstraintsUniqueEventTypeProjectInitialization implements IConstraint {
    @Override
    public void define(IConstraintBuilder builder) {
        builder.uniqueFor(ConstraintsUniqueEventTypeProjectInitialized.class, "");
    }
}
```
