```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.observation.Reactor

@EventType(id = "side-effects-activity-logged")
data class ActivityLogged(val isbn: String)

@Reactor
class MixedSideEffectsReactor {
    // A bare event uses the triggering event's EventSourceId; an EventForEventSourceId keeps its
    // own. Mix both in a single List and they are appended together as one transaction.
    fun bookReserved(event: BookReserved, context: EventContext): List<Any> = listOf(
        ActivityLogged(event.isbn),
        EventForEventSourceId(event.memberId, MemberActivityRecorded(event.isbn))
    )
}
```
