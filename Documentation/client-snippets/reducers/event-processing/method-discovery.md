```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel
import java.util.UUID

@EventType
data class EventProcessingMethodDiscoveryOrderCreated(val orderId: UUID)

@EventType
data class EventProcessingMethodDiscoveryItemAdded(val price: Double)

@ReadModel
data class EventProcessingMethodDiscoveryOrderSummary(val orderId: UUID = UUID(0, 0), val total: Double = 0.0)

@Reducer
class EventProcessingMethodDiscoveryOrderSummaryReducer {
    // Discovery matches on the first parameter's type, not the method name - a descriptive name
    // such as `created` for an OrderCreated event is good practice, not a requirement.
    fun created(event: EventProcessingMethodDiscoveryOrderCreated, current: EventProcessingMethodDiscoveryOrderSummary?, context: EventContext) =
        EventProcessingMethodDiscoveryOrderSummary(event.orderId, 0.0)

    fun itemAdded(event: EventProcessingMethodDiscoveryItemAdded, current: EventProcessingMethodDiscoveryOrderSummary?, context: EventContext): EventProcessingMethodDiscoveryOrderSummary? {
        if (current == null) return null

        return current.copy(total = current.total + event.price)
    }
}
```
