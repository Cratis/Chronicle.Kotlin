```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import java.time.Instant

@EventType(id = "reducers-getting-started-order-created")
data class ReducersGettingStartedOrderCreated(val orderId: String)

@EventType(id = "reducers-getting-started-item-added-to-order")
data class ReducersGettingStartedItemAddedToOrder(val price: Double, val quantity: Int)

@EventType(id = "reducers-getting-started-item-removed-from-order")
data class ReducersGettingStartedItemRemovedFromOrder(val price: Double, val quantity: Int)

@Reducer
class ReducersGettingStartedOrderSummaryReducer {
    fun created(event: ReducersGettingStartedOrderCreated): ReducersGettingStartedOrderSummary =
        ReducersGettingStartedOrderSummary(
            orderId = event.orderId,
            totalAmount = 0.0,
            itemCount = 0,
            lastUpdated = Instant.now()
        )

    fun itemAdded(
        event: ReducersGettingStartedItemAddedToOrder,
        current: ReducersGettingStartedOrderSummary?
    ): ReducersGettingStartedOrderSummary? {
        if (current == null) return null // Skip if order not created yet

        return current.copy(
            totalAmount = current.totalAmount + (event.price * event.quantity),
            itemCount = current.itemCount + event.quantity,
            lastUpdated = Instant.now()
        )
    }

    fun itemRemoved(
        event: ReducersGettingStartedItemRemovedFromOrder,
        current: ReducersGettingStartedOrderSummary?
    ): ReducersGettingStartedOrderSummary? {
        if (current == null) return null // Skip if order not created yet

        return current.copy(
            totalAmount = current.totalAmount - (event.price * event.quantity),
            itemCount = current.itemCount - event.quantity,
            lastUpdated = Instant.now()
        )
    }
}
```
