```kotlin title="Declarative FromAll"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "user-created-declarative-all")
data class UserCreatedDeclarativeAll(val name: String, val email: String)

@EventType(id = "user-email-changed-declarative-all")
data class UserEmailChangedDeclarativeAll(val email: String)

data class UserProfileDeclarativeAll(
    val name: String = "",
    val email: String = "",
    val lastUpdated: String = ""
)

class UserProfileDeclarativeAllProjection : IProjectionFor<UserProfileDeclarativeAll> {
    override fun define(builder: IProjectionBuilderFor<UserProfileDeclarativeAll>) {
        builder
            .from(UserCreatedDeclarativeAll::class)
            .from(UserEmailChangedDeclarativeAll::class)
            .fromAll { it.set(UserProfileDeclarativeAll::lastUpdated).toEventContextProperty("occurred") }
    }
}
```
