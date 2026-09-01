```kotlin title="Map multiple context fields"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class AccountTouchedDeclarativeEvery(val reason: String)

data class AccountAuditDeclarativeEvery(
    val lastUpdated: String = "",
    val lastEventSequence: String = "",
    val lastCorrelationId: String = ""
)

class AccountAuditDeclarativeEveryProjection : IProjectionFor<AccountAuditDeclarativeEvery> {
    override fun define(builder: IProjectionBuilderFor<AccountAuditDeclarativeEvery>) {
        builder
            .from(AccountTouchedDeclarativeEvery::class)
            .fromEvery {
                it.set(AccountAuditDeclarativeEvery::lastUpdated).toEventContextProperty("occurred")
                it.set(AccountAuditDeclarativeEvery::lastEventSequence).toEventContextProperty("sequenceNumber")
                it.set(AccountAuditDeclarativeEvery::lastCorrelationId).toEventContextProperty("correlationId")
            }
    }
}
```
