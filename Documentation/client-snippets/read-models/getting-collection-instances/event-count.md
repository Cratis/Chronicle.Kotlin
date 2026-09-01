```kotlin
import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class EventCountOrder(val status: String = "", val total: Double = 0.0)

/**
 * Caps the replay to the first 1,000 events - faster, but can return incomplete state if the
 * cap cuts off events that matter.
 */
suspend fun replayCappedOrders(store: IEventStore): List<EventCountOrder> =
    store.readModels.getInstances(EventCountOrder::class, eventCount = 1_000)
```
