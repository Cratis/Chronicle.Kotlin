```kotlin title="Multiple FromEvery declarations"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "user-changed-declarative-every-multiple")
data class UserChangedDeclarativeEveryMultiple(val name: String)

data class UserAuditDeclarativeEveryMultiple(
    val name: String = "",
    val lastUpdated: String = "",
    val modifiedBy: String = ""
)

class UserAuditDeclarativeEveryMultipleProjection : IProjectionFor<UserAuditDeclarativeEveryMultiple> {
    override fun define(builder: IProjectionBuilderFor<UserAuditDeclarativeEveryMultiple>) {
        builder
            .from(UserChangedDeclarativeEveryMultiple::class)
            .fromEvery { it.set(UserAuditDeclarativeEveryMultiple::lastUpdated).toEventContextProperty("occurred") }
            .fromEvery { it.set(UserAuditDeclarativeEveryMultiple::modifiedBy).toEventContextProperty("causedBy") }
    }
}
```
