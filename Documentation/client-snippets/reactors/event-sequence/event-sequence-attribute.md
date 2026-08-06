```kotlin
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.EventSequence
import io.cratis.chronicle.observation.Reactor

@EventType(id = "reactors-event-sequence-order-delivered")
data class ReactorsEventSequenceOrderDelivered(val orderId: String)

@Reactor
@EventSequence("outbox")
class ReactorsEventSequenceDeliveryNotifications {
    fun delivered(event: ReactorsEventSequenceOrderDelivered) {
        // Observed from the "outbox" sequence, without giving up the conventional identifier.
    }
}
```
