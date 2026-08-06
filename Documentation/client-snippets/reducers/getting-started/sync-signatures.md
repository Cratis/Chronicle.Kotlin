```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reducer
import io.cratis.chronicle.readModels.ReadModel

@EventType(id = "reducers-signatures-opened")
data class ReducersSignaturesOpened(val orderId: String)

@EventType(id = "reducers-signatures-item-added")
data class ReducersSignaturesItemAdded(val amount: Double)

@EventType(id = "reducers-signatures-closed")
data class ReducersSignaturesClosed(val reason: String)

@ReadModel
data class ReducersSignaturesOrder(
    val orderId: String = "",
    val total: Double = 0.0,
    val closedAt: String = ""
)

@Reducer
class ReducersSignaturesOrderReducer {
    // (event) - the state so far is not needed
    fun opened(event: ReducersSignaturesOpened) =
        ReducersSignaturesOrder(orderId = event.orderId)

    // (event, current) - current is null until the first event for an event source
    fun itemAdded(event: ReducersSignaturesItemAdded, current: ReducersSignaturesOrder?) =
        current?.copy(total = current.total + event.amount)

    // (event, current, context) - context carries the event's metadata
    fun closed(
        event: ReducersSignaturesClosed,
        current: ReducersSignaturesOrder?,
        context: EventContext
    ) = current?.copy(closedAt = context.occurred.toString())
}
```
