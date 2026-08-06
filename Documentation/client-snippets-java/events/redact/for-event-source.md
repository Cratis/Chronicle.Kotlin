```java
import io.cratis.chronicle.EventStore;

import java.util.List;

import io.cratis.chronicle.java.EventLogJavaBridge;

class EventsRedactForEventSource {
    // Permanently redacts every event for a single event source — a full "right to be forgotten"
    // erasure. Pass an empty list to redact every event type for that source.
    void redactAllEventsForCustomer(EventStore store, String customerId) {
        EventLogJavaBridge.redactForEventSource(store.getEventLog(), customerId, "GDPR erasure request", List.of());
    }
}
```
