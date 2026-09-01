```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.NotRewindable
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class MbNotRewindableAuditEvent(val message: String, val occurredAt: String)

@ReadModel
@FromEvent(MbNotRewindableAuditEvent::class)
@NotRewindable
data class MbNotRewindableAuditLog(
    @SetFrom("message", MbNotRewindableAuditEvent::class)
    val message: String = "",

    @SetFrom("occurredAt", MbNotRewindableAuditEvent::class)
    val timestamp: String = ""
)
```
