```java
import io.cratis.chronicle.constraints.IConstraint;
import io.cratis.chronicle.constraints.IConstraintBuilder;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.java.UniqueConstraintBuilderJavaBridge;

@EventType(id = "constraints-unique-casing-user-registered")
record ConstraintsUniqueCasingUserRegistered(String email) {
}

class ConstraintsUniqueCasingEmail implements IConstraint {
    @Override
    public void define(IConstraintBuilder builder) {
        builder.unique(unique -> {
            UniqueConstraintBuilderJavaBridge.on(unique, ConstraintsUniqueCasingUserRegistered.class, ConstraintsUniqueCasingUserRegistered::email)
                .ignoreCasing();
            return null;
        });
    }
}
```
