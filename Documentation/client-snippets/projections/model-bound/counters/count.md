```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.Count
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "mb-counters-order-placed")
class MbCountersOrderPlaced

@EventType(id = "mb-counters-order-cancelled")
class MbCountersOrderCancelled

@ReadModel
@FromEvent(MbCountersOrderPlaced::class)
@FromEvent(MbCountersOrderCancelled::class)
data class MbCountersEventMetrics(
    @Count(MbCountersOrderPlaced::class)
    val totalOrders: Int = 0,

    @Count(MbCountersOrderCancelled::class)
    val cancelledOrders: Int = 0
)
```
