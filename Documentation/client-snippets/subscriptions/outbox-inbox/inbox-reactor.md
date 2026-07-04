```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor

@EventType(id = "subscriptions-outbox-inbox-order-placed")
data class SubscriptionsOutboxInboxOrderPlaced(val orderId: String)

@Reactor
class SubscriptionsOutboxInboxIncomingOrdersReactor {
    fun orderPlaced(event: SubscriptionsOutboxInboxOrderPlaced, context: EventContext) {
        // Handles OrderPlaced events from any subscribed source event store
        process(event.orderId)
    }

    private fun process(orderId: String) {}
}
```
