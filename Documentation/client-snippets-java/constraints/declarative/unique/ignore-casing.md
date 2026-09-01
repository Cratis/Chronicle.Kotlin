```java
import io.cratis.chronicle.constraints.IConstraint;
import io.cratis.chronicle.constraints.IConstraintBuilder;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.UniqueConstraintBuilderJavaBridge;

@EventType
record ConstraintsUniqueCasingUserRegistered(String email) {}

class ConstraintsUniqueCasingEmail implements IConstraint {
    @Override
    public void define(IConstraintBuilder builder) {
        builder.unique(unique -> {
            UniqueConstraintBuilderJavaBridge.on(unique, ConstraintsUniqueCasingUserRegistered.class, "email")
                .ignoreCasing();
            return null; // Java lambda returning Unit
        });
    }
}
```
