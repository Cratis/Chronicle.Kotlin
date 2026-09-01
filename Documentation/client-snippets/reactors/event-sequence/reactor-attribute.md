```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.Reactor

@EventType
data class ReactorsEventSequenceOrderShipped(val orderId: String)

@Reactor(id = "shipping-notifications", eventSequence = "outbox")
class ReactorsEventSequenceShippingNotifications {
    fun shipped(event: ReactorsEventSequenceOrderShipped) {
        // Observed from the "outbox" sequence rather than the default event log.
    }
}
```
