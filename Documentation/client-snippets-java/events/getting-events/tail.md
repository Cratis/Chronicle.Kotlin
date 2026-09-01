```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.eventSequences.AppendedEvent;

import java.util.List;

import io.cratis.chronicle.java.EventLogJavaBridge;

class EventsGettingEventsTail {
    // Reads from a computed start position and trims in memory to the requested count.
    // The tail sequence number bridge returns -1 when the sequence is unavailable (empty).
    List<AppendedEvent> readLast(EventStore store, int count) {
        long tail = EventLogJavaBridge.getTailSequenceNumber(store.getEventLog(), null);
        long start = tail >= 0 && tail >= count - 1 ? tail - (count - 1) : 0;

        List<AppendedEvent> events = EventLogJavaBridge.getFromSequenceNumber(store.getEventLog(), start, null, null);
        return events.size() > count ? events.subList(events.size() - count, events.size()) : events;
    }
}
```
