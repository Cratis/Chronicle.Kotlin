```kotlin title="Equivalent explicit mappings"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "explicit-convention-user-registered")
data class ExplicitConventionUserRegistered(
    val name: String,
    val email: String,
    val registeredAt: String
)

@ReadModel
@FromEvent(ExplicitConventionUserRegistered::class)
data class ExplicitConventionUser(
    @SetFrom("name")
    val name: String = "",

    @SetFrom("email")
    val email: String = "",

    @SetFrom("registeredAt")
    val registeredAt: String = ""
)
```
