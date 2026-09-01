```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.SetFrom
import io.cratis.chronicle.projections.SetFromContext
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class OrderPlacedForAudit(val customerName: String)

@ReadModel
@FromEvent(OrderPlacedForAudit::class)
data class AuditedOrder(
    @SetFrom("customerName", OrderPlacedForAudit::class)
    val customerName: String = "",

    @SetFromContext(OrderPlacedForAudit::class, contextProperty = "occurred")
    val orderedAt: String = ""
)
```
