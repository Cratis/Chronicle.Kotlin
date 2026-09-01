```java
import io.cratis.chronicle.constraints.RemoveConstraint;
import io.cratis.chronicle.events.EventType;

@EventType
@RemoveConstraint("UniqueEmail")
record ConstraintsModelBoundUniqueUserRemoved(String userId) {}
```
