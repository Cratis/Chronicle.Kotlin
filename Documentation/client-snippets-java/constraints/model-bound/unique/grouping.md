```java
import io.cratis.chronicle.constraints.Unique;
import io.cratis.chronicle.events.EventType;

@EventType
record ConstraintsModelBoundUniqueUserRegistered(@Unique(id = "UniqueEmail") String email, String displayName) {}

@EventType
record ConstraintsModelBoundUniqueUserEmailChanged(@Unique(id = "UniqueEmail") String newEmail) {}
```
