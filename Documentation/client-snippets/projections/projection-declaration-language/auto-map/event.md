```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class PdlAutoMapUserRegistered(
    val name: String,
    val email: String,
    val age: Int
)
```
