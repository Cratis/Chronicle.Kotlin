```java
import io.cratis.chronicle.constraints.Unique;
import io.cratis.chronicle.events.EventType;

@EventType
record ConstraintsModelBoundUniqueProjectCreated(@Unique String name, String description) {}
```
