```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.observation.Reactor

@EventType
data class MemberActivityRecorded(val isbn: String)

@Reactor
class ReservationReactor {
    fun bookReserved(event: BookReserved, context: EventContext) =
        EventForEventSourceId(event.memberId, MemberActivityRecorded(event.isbn))
}
```
