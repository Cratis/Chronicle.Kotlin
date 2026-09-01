```kotlin
import io.cratis.chronicle.events.EventType

@EventType(id = "dec-from-event-sequence-order-created")
data class DecFromEventSequenceOrderCreated(
    val orderNumber: String,
    val customerId: String,
    val totalAmount: Double
)

@EventType(id = "dec-from-event-sequence-order-updated")
data class DecFromEventSequenceOrderUpdated(
    val orderNumber: String,
    val newTotalAmount: Double
)

@EventType(id = "dec-from-event-sequence-order-shipped")
data class DecFromEventSequenceOrderShipped(
    val orderNumber: String,
    val shippedAt: String
)
```
