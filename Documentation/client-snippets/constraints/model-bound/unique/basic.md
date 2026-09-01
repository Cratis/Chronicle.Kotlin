```kotlin
import io.cratis.chronicle.constraints.Unique
import io.cratis.chronicle.events.EventType

@EventType
data class ConstraintsModelBoundUniqueProjectCreated(@Unique val name: String, val description: String)
```
