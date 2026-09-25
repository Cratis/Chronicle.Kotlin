```kotlin
// Requires io.cratis:chronicle 6.5.0 or later.
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.observation.Reactor

@EventType
data class ActivityLogged(val isbn: String)

@Reactor
class MixedSideEffectsReactor {
    // A bare event uses the triggering event's EventSourceId; an EventForEventSourceId keeps its
    // own. The returned list is appended as one atomic batch, even across event sources.
    fun bookReserved(event: BookReserved, context: EventContext): List<Any> = listOf(
        ActivityLogged(event.isbn),
        EventForEventSourceId(event.memberId, MemberActivityRecorded(event.isbn))
    )
}
```
