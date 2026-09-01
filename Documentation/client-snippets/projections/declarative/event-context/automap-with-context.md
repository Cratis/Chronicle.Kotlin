```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor

@EventType(id = "dec-event-context-user-action")
data class DecEventContextUserAction(val userId: String, val actionType: String)

class DecEventContextAuditTrailProjection : IProjectionFor<DecEventContextAuditEntry> {
    override fun define(builder: IProjectionBuilderFor<DecEventContextAuditEntry>) {
        builder
            .autoMap()
            .from(DecEventContextUserAction::class) {
                it.set(DecEventContextAuditEntry::eventId).toEventContextProperty("sequenceNumber")
                it.set(DecEventContextAuditEntry::occurredAt).toEventContextProperty("occurred")
                it.set(DecEventContextAuditEntry::correlationId).toEventContextProperty("correlationId")
            }
    }
}
```
