```kotlin
import io.cratis.chronicle.constraints.RemoveConstraint
import io.cratis.chronicle.events.EventType

@EventType
@RemoveConstraint("UniqueEmail")
data class ConstraintsModelBoundUniqueUserRemoved(val userId: String)
```
