```java
import io.cratis.chronicle.constraints.Unique;
import io.cratis.chronicle.events.EventType;

// Model-bound equivalent: the explicit id names the constraint UniqueEmail.
@EventType
record ConstraintsUniqueNamedUserRegistered(@Unique(id = "UniqueEmail") String email) {}
```
