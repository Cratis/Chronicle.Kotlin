```kotlin title="Disable AutoMap"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "auto-map-disabled-account-registered")
data class AutoMapDisabledAccountRegistered(val accountName: String, val contactEmail: String)

data class AutoMapDisabledAccount(
    val name: String = "",
    val email: String = "",
    val createdAt: String = ""
)

class AutoMapDisabledAccountProjection : IProjectionFor<AutoMapDisabledAccount> {
    override fun define(builder: IProjectionBuilderFor<AutoMapDisabledAccount>) {
        builder
            .noAutoMap()
            .from(AutoMapDisabledAccountRegistered::class) {
                it.set(AutoMapDisabledAccount::name).toProperty("accountName")
                it.set(AutoMapDisabledAccount::email).toProperty("contactEmail")
                it.set(AutoMapDisabledAccount::createdAt).toEventContextProperty("occurred")
            }
    }
}
```
