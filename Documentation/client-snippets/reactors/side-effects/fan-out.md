```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.observation.Reactor

@EventType
data class FanOutStockDecreased(val isbn: String, val quantity: Int)

@Reactor
class ReservationFanOutReactor {
    // Fan-out events are appended one at a time, in order; an earlier event can be appended even if a later one is rejected.
    fun bookReserved(event: BookReserved, context: EventContext): List<EventForEventSourceId> = listOf(
        EventForEventSourceId(event.memberId, MemberActivityRecorded(event.isbn)),
        EventForEventSourceId(event.isbn, FanOutStockDecreased(event.isbn, 1))
    )
}
```
