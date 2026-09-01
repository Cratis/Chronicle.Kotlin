```kotlin title="Map the event source id with FromAll"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class AccountOpenedDeclarativeAll(val ownerName: String)

data class AccountSummaryDeclarativeAll(
    val accountId: String = "",
    val ownerName: String = ""
)

class AccountSummaryDeclarativeAllProjection : IProjectionFor<AccountSummaryDeclarativeAll> {
    override fun define(builder: IProjectionBuilderFor<AccountSummaryDeclarativeAll>) {
        builder
            .from(AccountOpenedDeclarativeAll::class)
            .fromAll { it.set(AccountSummaryDeclarativeAll::accountId).toEventSourceId() }
    }
}
```
