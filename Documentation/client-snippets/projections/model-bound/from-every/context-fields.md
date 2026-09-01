```kotlin title="Track audit metadata from every event"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.FromEvery
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "auditable-inventory-changed-for-every")
data class AuditableInventoryChangedForEvery(val reason: String)

@ReadModel
@FromEvent(AuditableInventoryChangedForEvery::class)
data class AuditableInventoryStatusFromEvery(
    @FromEvery(contextProperty = "occurred")
    val lastModified: String = "",

    @FromEvery(contextProperty = "sequenceNumber")
    val lastEventSequence: String = "",

    @FromEvery(contextProperty = "correlationId")
    val lastCorrelationId: String = ""
)
```
