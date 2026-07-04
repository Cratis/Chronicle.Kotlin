```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor

@EventType(id = "subscriptions-explicit-order-placed")
data class SubscriptionsExplicitOrderPlaced(val orderId: String, val amount: Double)

@Reactor
class SubscriptionsExplicitIncomingOrdersReactor {
    fun orderPlaced(event: SubscriptionsExplicitOrderPlaced, context: EventContext) {
        handleIncomingOrder(event.orderId, event.amount)
    }

    private fun handleIncomingOrder(id: String, amount: Double) {}
}
```
