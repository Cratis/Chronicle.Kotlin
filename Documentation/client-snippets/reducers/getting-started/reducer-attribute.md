```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@ReadModel
data class ReducersGettingStartedAttributeOrderSummary(val orderId: String = "")

@Reducer(id = "order-summary", eventSequence = "outbox", isActive = false)
class ReducersGettingStartedAttributeOrderSummaryReducer {
    fun on(event: ReducersGettingStartedAttributeOrderPlaced) =
        ReducersGettingStartedAttributeOrderSummary(event.orderId)
}

@EventType
data class ReducersGettingStartedAttributeOrderPlaced(val orderId: String = "")
```
