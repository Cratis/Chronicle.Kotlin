```kotlin title="Declarative FromEvery"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class UserCreatedDeclarativeEvery(val name: String, val email: String)

@EventType
data class UserEmailChangedDeclarativeEvery(val email: String)

data class UserProfileDeclarativeEvery(
    val name: String = "",
    val email: String = "",
    val lastUpdated: String = ""
)

class UserProfileDeclarativeEveryProjection : IProjectionFor<UserProfileDeclarativeEvery> {
    override fun define(builder: IProjectionBuilderFor<UserProfileDeclarativeEvery>) {
        builder
            .from(UserCreatedDeclarativeEvery::class)
            .from(UserEmailChangedDeclarativeEvery::class)
            .fromEvery { it.set(UserProfileDeclarativeEvery::lastUpdated).toEventContextProperty("occurred") }
    }
}
```
