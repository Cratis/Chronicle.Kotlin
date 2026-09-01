```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.observation.EventSourceType
import io.cratis.chronicle.observation.EventStreamType
import io.cratis.chronicle.observation.FilterEventsByTag
import io.cratis.chronicle.observation.Reactor

@EventType
data class ReactorsFilteringShipmentDispatched(val trackingNumber: String)

@Reactor
@FilterEventsByTag("priority")
@EventSourceType("Order")
@EventStreamType("Fulfilment")
class ReactorsFilteringShipmentNotifier {
    fun dispatched(
        event: ReactorsFilteringShipmentDispatched,
        context: EventContext
    ) {
        // Every filter has to match: the tag, the event source type, and the stream type.
    }
}
```
