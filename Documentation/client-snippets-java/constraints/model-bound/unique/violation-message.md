```java
import io.cratis.chronicle.constraints.Unique;
import io.cratis.chronicle.events.EventType;

@EventType
record ConstraintsModelBoundUniqueMessageProjectCreated(
    @Unique(message = "A project with this name already exists.") String name, String description) {}
```
