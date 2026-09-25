```java
import io.cratis.chronicle.constraints.RemoveConstraint;
import io.cratis.chronicle.constraints.Unique;
import io.cratis.chronicle.events.EventType;

// Model-bound equivalent: the fluent unique builder has no removedWith() on the JVM.
@EventType
record ConstraintsUniqueProjectCreated(@Unique(id = "ConstraintsUniqueProjectName") String name) {}

@EventType
@RemoveConstraint("ConstraintsUniqueProjectName")
record ConstraintsUniqueProjectRemoved() {}
```
