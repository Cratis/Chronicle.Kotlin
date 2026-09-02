```java
import io.cratis.chronicle.constraints.Constraint;
import io.cratis.chronicle.constraints.IConstraint;
import io.cratis.chronicle.constraints.IConstraintBuilder;
import io.cratis.chronicle.events.EventType;

import io.cratis.chronicle.java.UniqueConstraintBuilderJavaBridge;

@EventType
record ConstraintsUniqueScopedUserRegistered(String userId, String email) {}

// Scopes uniqueness checking to be per event source type rather than globally across the whole
// event store. perEventSourceType()/perEventStreamType()/perEventStreamId() each narrow a
// different dimension; combine them for multiple dimensions at once.
@Constraint
class ConstraintsUniqueScopedEmail implements IConstraint {
    @Override
    public void define(IConstraintBuilder builder) {
        builder.perEventSourceType().unique(unique -> {
            UniqueConstraintBuilderJavaBridge.on(unique, ConstraintsUniqueScopedUserRegistered.class, "email")
                .ignoreCasing()
                .withMessage("Email must be unique per event source type.");
        });
    }
}
```
