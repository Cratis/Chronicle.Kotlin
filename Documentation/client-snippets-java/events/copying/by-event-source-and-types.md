```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;

import java.util.List;

import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record CopyingAccountOpened(String owner) {}

@EventType
record CopyingAddressChanged(String street, String city) {}

class EventsCopyingByEventSourceAndTypes {
    // Copies only the given event types for an event source, leaving every other event type behind.
    String copyAccountDetails(EventStore store, String sourceId, String targetId) {
        return EventLogJavaBridge.copyForEventSource(
            store.getEventLog(),
            sourceId,
            targetId,
            List.of(CopyingAccountOpened.class, CopyingAddressChanged.class));
    }
}
```
