```kotlin title="Partial event shapes"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class ConventionPartialUserRegistered(
    val email: String
)

@EventType
data class ConventionPartialUserCompleted(
    val firstName: String,
    val lastName: String,
    val phone: String
)

@ReadModel
@FromEvent(ConventionPartialUserRegistered::class)
@FromEvent(ConventionPartialUserCompleted::class)
data class ConventionPartialUser(
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phone: String = ""
)
```
