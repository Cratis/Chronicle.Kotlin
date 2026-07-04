```kotlin
import io.cratis.chronicle.events.EventType
import java.time.OffsetDateTime

@EventType(id = "dec-simple-user-created")
data class DecSimpleUserCreated(val name: String, val email: String, val createdAt: OffsetDateTime)
```
