```kotlin
import io.cratis.chronicle.events.EventType

@EventType
data class DecFromEventSequenceOrderCreated(
    val orderNumber: String,
    val customerId: String,
    val totalAmount: Double
)

@EventType
data class DecFromEventSequenceOrderUpdated(
    val orderNumber: String,
    val newTotalAmount: Double
)

@EventType
data class DecFromEventSequenceOrderShipped(
    val orderNumber: String,
    val shippedAt: String
)
```
