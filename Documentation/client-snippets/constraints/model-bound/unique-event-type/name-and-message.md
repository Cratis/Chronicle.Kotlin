```kotlin
import io.cratis.chronicle.constraints.Unique
import io.cratis.chronicle.events.EventType

@EventType
// Not sent to the kernel by io.cratis:chronicle 6.4.0; a violation carries the kernel's own message.
@Unique(id = "UniqueUser", message = "A user with this identity has already been registered.")
data class ConstraintsModelBoundUniqueEventTypeNamedUserRegistered(val email: String, val displayName: String)
```
