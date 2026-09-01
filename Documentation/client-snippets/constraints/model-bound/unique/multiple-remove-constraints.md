```kotlin
import io.cratis.chronicle.constraints.RemoveConstraint
import io.cratis.chronicle.events.EventType

@EventType
@RemoveConstraint("UniqueEmail")
@RemoveConstraint("UniqueUsername")
data class ConstraintsModelBoundUniqueMultiRemoveUserRemoved(val userId: String)
```
