```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import java.time.Instant
import java.util.UUID

@EventType(id = "event-processing-order-created-for-status")
data class EventProcessingOrderCreatedForStatus(val orderId: UUID)

@EventType(id = "event-processing-order-paid")
data class EventProcessingOrderPaid(val orderId: UUID)

@EventType(id = "event-processing-order-shipped")
data class EventProcessingOrderShipped(val orderId: UUID)

@EventType(id = "event-processing-order-delivered")
data class EventProcessingOrderDelivered(val orderId: UUID)

@EventType(id = "event-processing-order-cancelled")
data class EventProcessingOrderCancelled(val orderId: UUID)

@ReadModel
data class EventProcessingOrderStatus(val state: String = "", val lastUpdated: Instant = Instant.EPOCH)

@Reducer
class EventProcessingOrderStatusReducer {
    fun created(event: EventProcessingOrderCreatedForStatus, current: EventProcessingOrderStatus?, context: EventContext) =
        EventProcessingOrderStatus("Created", context.occurred)

    fun paid(event: EventProcessingOrderPaid, current: EventProcessingOrderStatus?, context: EventContext) =
        EventProcessingOrderStatus("Paid", context.occurred)

    fun shipped(event: EventProcessingOrderShipped, current: EventProcessingOrderStatus?, context: EventContext) =
        EventProcessingOrderStatus("Shipped", context.occurred)

    fun delivered(event: EventProcessingOrderDelivered, current: EventProcessingOrderStatus?, context: EventContext) =
        EventProcessingOrderStatus("Delivered", context.occurred)

    fun cancelled(event: EventProcessingOrderCancelled, current: EventProcessingOrderStatus?, context: EventContext) =
        EventProcessingOrderStatus("Cancelled", context.occurred)
}
```
