```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.EventForEventSourceId;
import io.cratis.chronicle.observation.Reactor;

@EventType
record MemberActivityRecorded(String isbn) {}

@Reactor
class ReservationReactor {
    EventForEventSourceId bookReserved(BookReserved event, EventContext context) {
        return new EventForEventSourceId(event.memberId(), new MemberActivityRecorded(event.isbn()));
    }
}
```
