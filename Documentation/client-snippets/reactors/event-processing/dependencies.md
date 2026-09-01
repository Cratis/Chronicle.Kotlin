```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "event-processing-order-placed")
data class EventProcessingOrderPlaced(val orderId: String)

@ReadModel
data class EventProcessingOrder(val id: String = "", val total: Double = 0.0)

interface EventProcessingShippingService {
    suspend fun schedule(order: EventProcessingOrder)
}

@Reactor
class EventProcessingOrderProcessor(private val shipping: EventProcessingShippingService) {
    // `order` is resolved by Chronicle itself: any parameter whose type carries @ReadModel is
    // materialized on demand, keyed by the triggering event's EventSourceId - strongly consistent
    // as of this handler call. It is null until something has been projected for that key.
    suspend fun orderPlaced(event: EventProcessingOrderPlaced, order: EventProcessingOrder?, context: EventContext) {
        if (order != null) shipping.schedule(order)
    }
}
```
