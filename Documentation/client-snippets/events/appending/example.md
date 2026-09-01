```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.events.EventType

@EventType
data class OrderPlaced(
    val customerId: String,
    val total: Double
)

class CheckoutService(private val store: IEventStore) {
    suspend fun placeOrder(orderId: String, customerId: String, total: Double) {
        val result = store.eventLog.append(
            orderId,
            OrderPlaced(customerId, total)
        )

        if (!result.isSuccess) {
            // Decide whether to retry or surface a conflict to the caller.
        }
    }
}
```
