```java
import io.cratis.chronicle.constraints.RemoveConstraint;
import io.cratis.chronicle.constraints.Unique;
import io.cratis.chronicle.events.EventType;

// Model-bound equivalent: only one open shift per employee/event source.
@EventType
@Unique(id = "ConstraintsUniqueEventTypeOneOpenShift")
record ConstraintsUniqueEventTypeShiftStarted(String location) {}

@EventType
@RemoveConstraint("ConstraintsUniqueEventTypeOneOpenShift")
record ConstraintsUniqueEventTypeShiftEnded() {}
```
