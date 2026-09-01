```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.eventSequences.EventForEventSourceId;
import io.cratis.chronicle.observation.Reactor;

@Reactor
class ExplicitMetadataReactor {
    EventForEventSourceId bookReserved(BookReserved event, EventContext context) {
        return new EventForEventSourceId(
            event.memberId(),
            new MemberActivityRecorded(event.isbn()),
            "members",
            null,
            null,
            java.util.List.of(),
            null,
            event.memberId(),
            java.util.List.of());
    }
}
```
