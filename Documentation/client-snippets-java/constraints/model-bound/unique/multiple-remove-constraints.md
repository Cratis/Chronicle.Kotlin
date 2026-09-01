```java
import io.cratis.chronicle.constraints.RemoveConstraint;
import io.cratis.chronicle.events.EventType;

@EventType
@RemoveConstraint("UniqueEmail")
@RemoveConstraint("UniqueUsername")
record ConstraintsModelBoundUniqueMultiRemoveUserRemoved(String userId) {}
```
