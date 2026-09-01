```kotlin
import io.cratis.chronicle.constraints.RemoveConstraint
import io.cratis.chronicle.events.EventType

@EventType
@RemoveConstraint("UniqueUser")
data class ConstraintsModelBoundUniqueEventTypeUserRemoved(val userId: String)
```
