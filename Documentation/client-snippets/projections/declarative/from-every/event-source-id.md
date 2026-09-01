```kotlin title="Map the event source id"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class AccountOpenedDeclarativeEvery(val ownerName: String)

data class AccountSummaryDeclarativeEvery(
    val accountId: String = "",
    val ownerName: String = ""
)

class AccountSummaryDeclarativeEveryProjection : IProjectionFor<AccountSummaryDeclarativeEvery> {
    override fun define(builder: IProjectionBuilderFor<AccountSummaryDeclarativeEvery>) {
        builder
            .from(AccountOpenedDeclarativeEvery::class)
            .fromEvery { it.set(AccountSummaryDeclarativeEvery::accountId).toEventSourceId() }
    }
}
```
