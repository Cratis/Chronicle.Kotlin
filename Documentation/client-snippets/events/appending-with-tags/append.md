```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.events.EventType

@EventType(id = "TaggedOrderPlaced")
data class TaggedOrderPlaced(val customerId: String, val total: Double)

class TaggedCheckoutService(private val eventStore: IEventStore) {
    suspend fun placeOrder(orderId: String, customerId: String, total: Double): AppendResult =
        eventStore.eventLog.append(
            orderId,
            TaggedOrderPlaced(customerId, total),
            AppendOptions(tags = listOf("checkout", "priority"))
        )
}
```
