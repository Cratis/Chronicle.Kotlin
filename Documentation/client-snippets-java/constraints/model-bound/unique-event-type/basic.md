```java
import io.cratis.chronicle.constraints.Unique;
import io.cratis.chronicle.events.EventType;

@EventType
@Unique
record ConstraintsModelBoundUniqueEventTypeUserRegistered(String email, String displayName) {}
```
