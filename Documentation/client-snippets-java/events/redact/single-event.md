```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.EventLogJavaBridge;

class EventsRedactSingleEvent {
    // Permanently rewrites the content of a single event. This is destructive — the original
    // content is gone once this returns — so only redact after a confirmed compliance request.
    void redactAddressEvent(EventStore store, long sequenceNumber) {
        EventLogJavaBridge.redact(store.getEventLog(), sequenceNumber, "GDPR erasure request");
    }
}
```
