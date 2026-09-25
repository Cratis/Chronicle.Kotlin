```java
import io.cratis.chronicle.constraints.RemoveConstraint;
import io.cratis.chronicle.constraints.Unique;
import io.cratis.chronicle.events.EventType;

// Model-bound equivalent: appending the cancellation releases the reference for reuse.
@EventType
record ConstraintsUniqueOrderPlaced(@Unique(id = "ConstraintsUniqueOrderReference") String reference) {}

@EventType
@RemoveConstraint("ConstraintsUniqueOrderReference")
record ConstraintsUniqueOrderCancelled() {}
```
