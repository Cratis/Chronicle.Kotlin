```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;

import java.util.List;

import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record RedactionPersonalDetailsRecorded(String name, String socialSecurityNumber) {}

@EventType
record RedactionAddressChanged(String street, String city) {}

class EventsRedactionByEventSourceAndTypes {
    // Redacts only the given event types for an event source, leaving every other event type intact.
    void redactPersonalData(EventStore store, String eventSourceId) {
        EventLogJavaBridge.redactForEventSource(
            store.getEventLog(),
            eventSourceId,
            "PII erasure",
            List.of(RedactionPersonalDetailsRecorded.class, RedactionAddressChanged.class));
    }
}
```
