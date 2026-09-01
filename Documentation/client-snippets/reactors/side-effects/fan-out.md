```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.observation.Reactor

@EventType(id = "side-effects-fan-out-stock-decreased")
data class FanOutStockDecreased(val isbn: String, val quantity: Int)

@Reactor
class ReservationFanOutReactor {
    // Fan out to several event source ids in one go - they are appended together as a single
    // transaction.
    fun bookReserved(event: BookReserved, context: EventContext): List<EventForEventSourceId> = listOf(
        EventForEventSourceId(event.memberId, MemberActivityRecorded(event.isbn)),
        EventForEventSourceId(event.isbn, FanOutStockDecreased(event.isbn, 1))
    )
}
```
