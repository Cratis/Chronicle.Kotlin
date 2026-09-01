```kotlin
import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.observation.Reactor
import io.cratis.chronicle.eventSequences.EventForEventSourceId

@Reactor
class ExplicitMetadataReactor {
    fun bookReserved(event: BookReserved, context: EventContext) =
        EventForEventSourceId(
            eventSourceId = event.memberId,
            event = MemberActivityRecorded(event.isbn),
            eventStreamType = "members",
            subject = event.memberId
        )
}
```
