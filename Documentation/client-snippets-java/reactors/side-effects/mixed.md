```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.EventForEventSourceId;
import io.cratis.chronicle.observation.Reactor;

import java.util.List;

@EventType
record ActivityLogged(String isbn) {}

@Reactor
class MixedSideEffectsReactor {
    // A bare event uses the triggering event's EventSourceId; an EventForEventSourceId keeps its
    // own. The returned list is appended as one atomic batch, even across event sources.
    List<Object> bookReserved(BookReserved event, EventContext context) {
        return List.of(
            new ActivityLogged(event.isbn()),
            new EventForEventSourceId(event.memberId(), new MemberActivityRecorded(event.isbn())));
    }
}
```
