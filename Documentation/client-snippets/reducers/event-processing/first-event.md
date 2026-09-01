```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import java.util.UUID

@EventType(id = "event-processing-order-created")
data class EventProcessingOrderCreated(val orderId: UUID)

@EventType(id = "event-processing-item-added")
data class EventProcessingItemAdded(val price: Double)

@ReadModel
data class EventProcessingOrderSummary(val orderId: UUID = UUID(0, 0), val total: Double = 0.0, val lastUpdated: java.time.Instant = java.time.Instant.EPOCH)

@Reducer
class EventProcessingOrderSummaryReducer {
    fun created(event: EventProcessingOrderCreated, current: EventProcessingOrderSummary?, context: EventContext) =
        EventProcessingOrderSummary(event.orderId, 0.0, context.occurred)

    fun itemAdded(event: EventProcessingItemAdded, current: EventProcessingOrderSummary?, context: EventContext): EventProcessingOrderSummary? {
        if (current == null) return null // Skip if no order exists

        return current.copy(total = current.total + event.price, lastUpdated = context.occurred)
    }
}
```
