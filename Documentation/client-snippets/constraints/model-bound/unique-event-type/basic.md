```kotlin
import io.cratis.chronicle.constraints.Unique
import io.cratis.chronicle.events.EventType

@EventType
@Unique
data class ConstraintsModelBoundUniqueEventTypeUserRegistered(val email: String, val displayName: String)
```
