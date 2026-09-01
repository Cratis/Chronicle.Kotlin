```kotlin title="AutoMap with explicit mappings"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "auto-map-account-opened")
data class AutoMapAccountOpened(val name: String, val email: String)

@EventType(id = "auto-map-account-email-changed")
data class AutoMapAccountEmailChanged(val email: String)

data class AutoMapAccount(
    val name: String = "",
    val email: String = "",
    val status: String = ""
)

class AutoMapAccountProjection : IProjectionFor<AutoMapAccount> {
    override fun define(builder: IProjectionBuilderFor<AutoMapAccount>) {
        builder
            .from(AutoMapAccountOpened::class) {
                it.set(AutoMapAccount::status).to { "Active" }
            }
            .from(AutoMapAccountEmailChanged::class)
    }
}
```
