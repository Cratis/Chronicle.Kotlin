```kotlin
import io.cratis.chronicle.constraints.RemoveConstraint
import io.cratis.chronicle.constraints.Unique
import io.cratis.chronicle.events.EventType

// Model-bound equivalent: appending the cancellation releases the reference for reuse.
@EventType
data class ConstraintsUniqueOrderPlaced(@Unique(id = "ConstraintsUniqueOrderReference") val reference: String)

@EventType
@RemoveConstraint("ConstraintsUniqueOrderReference")
class ConstraintsUniqueOrderCancelled
```
