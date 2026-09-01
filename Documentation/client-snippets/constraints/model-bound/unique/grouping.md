```kotlin
import io.cratis.chronicle.constraints.Unique
import io.cratis.chronicle.events.EventType

@EventType
data class ConstraintsModelBoundUniqueUserRegistered(
    @Unique(id = "UniqueEmail") val email: String,
    val displayName: String
)

@EventType
data class ConstraintsModelBoundUniqueUserEmailChanged(@Unique(id = "UniqueEmail") val newEmail: String)
```
