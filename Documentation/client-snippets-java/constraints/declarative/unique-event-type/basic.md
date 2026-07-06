```java
import io.cratis.chronicle.constraints.IConstraint;
import io.cratis.chronicle.constraints.IConstraintBuilder;
import io.cratis.chronicle.events.EventType;
import kotlin.jvm.JvmClassMappingKt;

@EventType(id = "constraints-unique-event-type-project-initialized")
class ConstraintsUniqueEventTypeProjectInitialized {
}

class ConstraintsUniqueEventTypeProjectInitialization implements IConstraint {
    @Override
    public void define(IConstraintBuilder builder) {
        builder.uniqueFor(
            JvmClassMappingKt.getKotlinClass(ConstraintsUniqueEventTypeProjectInitialized.class),
            "");
    }
}
```
