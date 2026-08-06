```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendedEvent;

import java.util.List;

import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record EventsForSourceAccountOpened(String accountId, String ownerName) {}

class EventsGettingEventsForEventSource {
    List<AppendedEvent> getAccountOpenedEvents(EventStore store, String accountId) {
        return EventLogJavaBridge.getForEventSourceIdAndEventTypes(
            store.getEventLog(), accountId, List.of(EventsForSourceAccountOpened.class));
    }
}
```
