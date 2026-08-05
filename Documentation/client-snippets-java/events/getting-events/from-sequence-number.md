```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.eventSequences.AppendedEvent;

import java.util.List;

import io.cratis.chronicle.java.EventLogJavaBridge;

class EventsGettingEventsFromSequenceNumber {
    List<AppendedEvent> getEventsSince(EventStore store, long sequenceNumber, String accountId) {
        return EventLogJavaBridge.getFromSequenceNumber(store.getEventLog(), sequenceNumber, accountId, null);
    }
}
```
