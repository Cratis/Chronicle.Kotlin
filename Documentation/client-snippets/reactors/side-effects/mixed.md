```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.observation.Reactor

@EventType
data class ActivityLogged(val isbn: String)

@Reactor
class MixedSideEffectsReactor {
    // A bare event uses the triggering event's EventSourceId; an EventForEventSourceId keeps its
    // own. Both are appended one at a time, in order; an earlier event can be appended even if a later one is rejected.
    fun bookReserved(event: BookReserved, context: EventContext): List<Any> = listOf(
        ActivityLogged(event.isbn),
        EventForEventSourceId(event.memberId, MemberActivityRecorded(event.isbn))
    )
}
```
