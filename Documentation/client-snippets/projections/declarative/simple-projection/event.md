```kotlin
import io.cratis.chronicle.events.EventType
import java.time.OffsetDateTime

@EventType
data class DecSimpleUserCreated(val name: String, val email: String, val createdAt: OffsetDateTime)
```
