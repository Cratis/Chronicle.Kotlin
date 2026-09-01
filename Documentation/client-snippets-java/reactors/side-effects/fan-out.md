```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.EventForEventSourceId;
import io.cratis.chronicle.observation.Reactor;

import java.util.List;

@EventType
record FanOutStockDecreased(String isbn, int quantity) {}

@Reactor
class ReservationFanOutReactor {
    // Fan out to several event source ids in one go - they are appended together as a single
    // transaction.
    List<EventForEventSourceId> bookReserved(BookReserved event, EventContext context) {
        return List.of(
            new EventForEventSourceId(event.memberId(), new MemberActivityRecorded(event.isbn())),
            new EventForEventSourceId(event.isbn(), new FanOutStockDecreased(event.isbn(), 1)));
    }
}
```
