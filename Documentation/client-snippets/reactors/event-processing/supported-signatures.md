```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor

@EventType(id = "event-processing-signatures-order-placed")
data class EventProcessingSignaturesOrderPlaced(val orderId: String)

@EventType(id = "event-processing-signatures-order-shipped")
data class EventProcessingSignaturesOrderShipped(val orderId: String)

@EventType(id = "event-processing-signatures-order-cancelled")
data class EventProcessingSignaturesOrderCancelled(val orderId: String)

@EventType(id = "event-processing-signatures-refund-issued")
data class EventProcessingSignaturesRefundIssued(val orderId: String, val amount: Double)

@EventType(id = "event-processing-signatures-order-archived")
data class EventProcessingSignaturesOrderArchived(val orderId: String)

@Reactor
class EventProcessingSignaturesReactor {
    // (event) - no metadata needed, no side effect
    fun placed(event: EventProcessingSignaturesOrderPlaced) {
    }

    // (event, context) - suspend, no side effect
    suspend fun shipped(event: EventProcessingSignaturesOrderShipped, context: EventContext) {
    }

    // (event) - returns a single side-effect event
    fun cancelled(event: EventProcessingSignaturesOrderCancelled) =
        EventProcessingSignaturesOrderArchived(event.orderId)

    // (event, context) - suspend, returns a list of side-effect events
    suspend fun refundIssued(
        event: EventProcessingSignaturesRefundIssued,
        context: EventContext
    ): List<Any> = listOf(EventProcessingSignaturesOrderArchived(event.orderId))
}
```
