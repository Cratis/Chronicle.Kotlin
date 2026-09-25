```kotlin
import io.cratis.chronicle.constraints.RemoveConstraint
import io.cratis.chronicle.constraints.Unique
import io.cratis.chronicle.events.EventType

// Model-bound equivalent: the fluent unique builder has no removedWith() on the JVM.
@EventType
data class ConstraintsUniqueProjectCreated(@Unique(id = "ConstraintsUniqueProjectName") val name: String)

@EventType
@RemoveConstraint("ConstraintsUniqueProjectName")
class ConstraintsUniqueProjectRemoved
```
