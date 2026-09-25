```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.readModels.ReadModel

@EventType
data class EventProcessingOrderPlaced(val orderId: String)

@ReadModel
data class EventProcessingOrder(val id: String = "", val total: Double = 0.0)

interface EventProcessingShippingService {
    suspend fun schedule(order: EventProcessingOrder)
}

@Reactor
class EventProcessingOrderProcessor(private val shipping: EventProcessingShippingService) {
    // `order` is looked up by the triggering event's event source id; a materialized read model
    // can lag this event or be null.
    suspend fun orderPlaced(event: EventProcessingOrderPlaced, order: EventProcessingOrder?, context: EventContext) {
        if (order != null) shipping.schedule(order)
    }
}
```
