```kotlin title="Custom key"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "convention-user-registered-with-key")
data class ConventionUserRegisteredWithKey(
    val userId: String,
    val name: String,
    val email: String
)

@ReadModel
@FromEvent(ConventionUserRegisteredWithKey::class, key = "userId")
data class ConventionUserById(
    val name: String = "",
    val email: String = ""
)
```
