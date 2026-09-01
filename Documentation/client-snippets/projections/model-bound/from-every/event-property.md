```kotlin title="Read a shared event property from every event"
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.FromEvery
import io.cratis.chronicle.readModels.ReadModel

enum class OrderStateFromEvery {
    New,
    Confirmed,
    Shipped
}

@EventType(id = "order-confirmed-for-every")
data class OrderConfirmedForEvery(val status: OrderStateFromEvery)

@EventType(id = "order-shipped-for-every")
data class OrderShippedForEvery(val status: OrderStateFromEvery)

@ReadModel
@FromEvent(OrderConfirmedForEvery::class)
@FromEvent(OrderShippedForEvery::class)
data class OrderStatusFromEvery(
    @FromEvery(property = "status")
    val currentStatus: OrderStateFromEvery = OrderStateFromEvery.New
)
```
