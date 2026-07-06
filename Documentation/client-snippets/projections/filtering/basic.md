```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.readModels.ReadModel
import java.math.BigDecimal
import java.time.OffsetDateTime

@EventType
data class FilteringOrderPlaced(val customerId: String, val totalAmount: BigDecimal)

@EventType
data class FilteringOrderShipped(val shippedAt: OffsetDateTime)

@ReadModel
@FromEvent(FilteringOrderPlaced::class)
@FromEvent(FilteringOrderShipped::class)
data class FilteringOrderSummary(
    val customerId: String = "",
    val totalAmount: BigDecimal = BigDecimal.ZERO,
    val shippedAt: OffsetDateTime? = null
)
```
