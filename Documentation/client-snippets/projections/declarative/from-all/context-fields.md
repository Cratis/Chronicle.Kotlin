```kotlin title="Map context fields with FromAll"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "account-touched-declarative-all")
data class AccountTouchedDeclarativeAll(val reason: String)

data class AccountAuditDeclarativeAll(
    val lastUpdated: String = "",
    val lastEventSequence: String = "",
    val lastCorrelationId: String = ""
)

class AccountAuditDeclarativeAllProjection : IProjectionFor<AccountAuditDeclarativeAll> {
    override fun define(builder: IProjectionBuilderFor<AccountAuditDeclarativeAll>) {
        builder
            .from(AccountTouchedDeclarativeAll::class)
            .fromAll {
                it.set(AccountAuditDeclarativeAll::lastUpdated).toEventContextProperty("occurred")
                it.set(AccountAuditDeclarativeAll::lastEventSequence).toEventContextProperty("sequenceNumber")
                it.set(AccountAuditDeclarativeAll::lastCorrelationId).toEventContextProperty("correlationId")
            }
    }
}
```
