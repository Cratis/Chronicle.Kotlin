```kotlin title="Specific context vs every event"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.FromEvery
import io.cratis.chronicle.projections.SetFromContext
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class OrderPlacedForLifecycle(val customerName: String)

@EventType
data class OrderShippedForLifecycle(val trackingNumber: String)

@ReadModel
@FromEvent(OrderPlacedForLifecycle::class)
@FromEvent(OrderShippedForLifecycle::class)
data class OrderLifecycle(
    @SetFromContext(OrderPlacedForLifecycle::class, contextProperty = "occurred")
    val placedAt: String = "",

    @SetFromContext(OrderShippedForLifecycle::class, contextProperty = "occurred")
    val shippedAt: String? = null,

    @FromEvery(contextProperty = "occurred")
    val lastModified: String = ""
)
```
