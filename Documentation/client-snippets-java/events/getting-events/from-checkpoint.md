```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.eventSequences.AppendedEvent;

import java.util.List;

import io.cratis.chronicle.java.EventLogJavaBridge;

class EventsGettingEventsFromCheckpoint {
    // Replays every event from a known checkpoint onwards, across all event sources.
    List<AppendedEvent> readFrom(EventStore store, long sequenceNumber) {
        return EventLogJavaBridge.getFromSequenceNumber(store.getEventLog(), sequenceNumber, null, null);
    }
}
```
