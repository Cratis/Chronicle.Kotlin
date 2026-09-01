```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType
data class DecNotRewindableUserLoginAttempt(val userId: String, val succeeded: Boolean)

@EventType
data class DecNotRewindablePermissionChange(val userId: String, val permission: String)

data class DecNotRewindableSecurityAuditEntry(
    val auditedAt: String = "",
    val sequenceNumber: Long = 0
)

class DecNotRewindableSecurityAuditProjection : IProjectionFor<DecNotRewindableSecurityAuditEntry> {
    override fun define(builder: IProjectionBuilderFor<DecNotRewindableSecurityAuditEntry>) {
        builder
            .notRewindable()
            .fromEvery {
                it.set(DecNotRewindableSecurityAuditEntry::auditedAt).toEventContextProperty("occurred")
                it.set(DecNotRewindableSecurityAuditEntry::sequenceNumber).toEventContextProperty("sequenceNumber")
            }
            .from(DecNotRewindableUserLoginAttempt::class)
            .from(DecNotRewindablePermissionChange::class)
    }
}
```
