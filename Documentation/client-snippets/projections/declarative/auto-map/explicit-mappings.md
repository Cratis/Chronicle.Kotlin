```kotlin title="AutoMap with explicit mappings"
// Requires io.cratis:chronicle 6.5.0 or later.
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class AutoMapAccountOpened(val name: String, val email: String)

@EventType
data class AutoMapAccountEmailChanged(val email: String)

data class AutoMapAccount(
    val name: String = "",
    val email: String = "",
    val status: String = "",
    val createdAt: String = ""
)

class AutoMapAccountProjection : IProjectionFor<AutoMapAccount> {
    override fun define(builder: IProjectionBuilderFor<AutoMapAccount>) {
        builder
            .from(AutoMapAccountOpened::class) {
                it.set(AutoMapAccount::status).toValue("Active")
                it.set(AutoMapAccount::createdAt).toEventContextProperty("occurred")
            }
            .from(AutoMapAccountEmailChanged::class)
    }
}
```
