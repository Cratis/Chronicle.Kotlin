```kotlin
import io.cratis.chronicle.events.EventType
import java.time.Instant

@EventType
data class CamelCasingUserRegistered(
    val firstName: String = "",
    val lastName: String = "",
    val emailAddress: String = "",
    val registrationDate: Instant = Instant.EPOCH
)
```
