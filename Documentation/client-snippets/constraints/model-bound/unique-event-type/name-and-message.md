```kotlin
// Requires io.cratis:chronicle 6.5.0 or later.
import io.cratis.chronicle.constraints.Unique
import io.cratis.chronicle.events.EventType

@EventType
@Unique(id = "UniqueUser", message = "A user with this identity has already been registered.")
data class ConstraintsModelBoundUniqueEventTypeNamedUserRegistered(val email: String, val displayName: String)
```
