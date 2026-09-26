```kotlin
// Requires io.cratis:chronicle 6.5.0 or later.
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.observation.Reactor

@EventType
data class FanOutStockDecreased(val isbn: String, val quantity: Int)

@Reactor
class ReservationFanOutReactor {
    // The returned list is appended as one atomic batch, even across event sources.
    fun bookReserved(event: BookReserved, context: EventContext): List<EventForEventSourceId> = listOf(
        EventForEventSourceId(event.memberId, MemberActivityRecorded(event.isbn)),
        EventForEventSourceId(event.isbn, FanOutStockDecreased(event.isbn, 1))
    )
}
```
