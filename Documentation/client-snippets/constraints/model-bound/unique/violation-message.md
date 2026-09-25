```kotlin
import io.cratis.chronicle.constraints.Unique
import io.cratis.chronicle.events.EventType

@EventType
data class ConstraintsModelBoundUniqueMessageProjectCreated(
    @Unique(message = "A project with this name already exists.") val name: String,
    val description: String
)
```
