```kotlin
import io.cratis.chronicle.constraints.Unique
import io.cratis.chronicle.events.EventType

@EventType
data class ConstraintsModelBoundUniqueMessageProjectCreated(
    // Not sent to the kernel by io.cratis:chronicle 6.4.0; a violation carries the kernel's own message.
    @Unique(message = "A project with this name already exists.") val name: String,
    val description: String
)
```
