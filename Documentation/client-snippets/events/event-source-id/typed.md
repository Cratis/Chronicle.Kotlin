```kotlin
import io.cratis.chronicle.concepts.EventSourceId
import io.cratis.chronicle.concepts.append
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.events.EventType
import java.util.UUID

// A strongly typed domain concept, stored as a string on the wire.
data class EventSourceIdCustomerId(override val value: String) : EventSourceId {
    companion object {
        fun new() = EventSourceIdCustomerId(UUID.randomUUID().toString())
    }
}

@EventType
data class EventSourceIdOrderPlaced(val customerId: EventSourceIdCustomerId, val total: Double)

class EventSourceIdOrderService(private val eventLog: IEventLog) {
    suspend fun placeOrder(total: Double) {
        val customerId = EventSourceIdCustomerId.new()
        eventLog.append(customerId, EventSourceIdOrderPlaced(customerId, total))
    }
}
```
