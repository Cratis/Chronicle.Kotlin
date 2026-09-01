```kotlin title="Convention-based set mapping"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class UserRegisteredForProfile(
    val name: String,
    val email: String
)

@ReadModel
@FromEvent(UserRegisteredForProfile::class)
data class UserProfile(
    @SetFrom
    val name: String = "",

    @SetFrom
    val email: String = ""
)
```
