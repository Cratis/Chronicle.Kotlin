```kotlin title="Combine specific mappings with every-event metadata"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.FromEvery
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class UserRegisteredForEvery(val name: String, val email: String)

@EventType
data class UserNameChangedForEvery(val newName: String)

@EventType
data class UserEmailChangedForEvery(val newEmail: String)

@ReadModel
@FromEvent(UserRegisteredForEvery::class)
@FromEvent(UserNameChangedForEvery::class)
@FromEvent(UserEmailChangedForEvery::class)
data class UserProfileFromEvery(
    @SetFrom("name", UserRegisteredForEvery::class)
    @SetFrom("newName", UserNameChangedForEvery::class)
    val name: String = "",

    @SetFrom("email", UserRegisteredForEvery::class)
    @SetFrom("newEmail", UserEmailChangedForEvery::class)
    val email: String = "",

    @FromEvery(contextProperty = "occurred")
    val lastUpdated: String = ""
)
```
