```kotlin title="Events used by composite key projections"
import io.cratis.chronicle.events.EventType

@EventType
data class CompositeOrderCreated(
    val customerId: String,
    val orderNumber: String,
    val customerName: String,
    val orderDate: String
)

@EventType
data class CompositeOrderShipped(
    val customerId: String,
    val orderNumber: String,
    val shippedDate: String
)

@EventType
data class CompositeUserAction(
    val userId: String,
    val action: String,
    val details: String
)
```
