```kotlin title="Convention-based mapping"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class ConventionUserRegistered(
    val name: String,
    val email: String,
    val registeredAt: String
)

@ReadModel
@FromEvent(ConventionUserRegistered::class)
data class ConventionUser(
    val name: String = "",
    val email: String = "",
    val registeredAt: String = ""
)
```
