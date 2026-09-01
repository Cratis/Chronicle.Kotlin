```kotlin title="Events used by composite key projections"
import io.cratis.chronicle.events.EventType

@EventType(id = "composite-order-created")
data class CompositeOrderCreated(
    val customerId: String,
    val orderNumber: String,
    val customerName: String,
    val orderDate: String
)

@EventType(id = "composite-order-shipped")
data class CompositeOrderShipped(
    val customerId: String,
    val orderNumber: String,
    val shippedDate: String
)

@EventType(id = "composite-user-action")
data class CompositeUserAction(
    val userId: String,
    val action: String,
    val details: String
)
```
