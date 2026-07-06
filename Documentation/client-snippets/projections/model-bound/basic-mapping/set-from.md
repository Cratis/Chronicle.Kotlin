```kotlin title="Model-bound set mapping"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "user-registered-for-contact")
data class UserRegisteredForContact(
    val name: String,
    val email: String
)

@ReadModel
@FromEvent(UserRegisteredForContact::class)
data class UserContact(
    @SetFrom("email", UserRegisteredForContact::class)
    val email: String = "",

    @SetFrom("name", UserRegisteredForContact::class)
    val name: String = ""
)
```
