```kotlin title="Multiple convention events"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class ConventionUserProfileCreated(
    val name: String,
    val email: String
)

@EventType
data class ConventionUserProfileUpdated(
    val name: String,
    val email: String,
    val phone: String
)

@ReadModel
@FromEvent(ConventionUserProfileCreated::class)
@FromEvent(ConventionUserProfileUpdated::class)
data class ConventionUserProfile(
    val name: String = "",
    val email: String = "",
    val phone: String = ""
)
```
