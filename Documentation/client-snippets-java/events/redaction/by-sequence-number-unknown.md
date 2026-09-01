```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.EventLogJavaBridge;

class EventsRedactionBySequenceNumberUnknown {
    // Redacts a single event by sequence number without stating a specific reason. "Unknown" is
    // the same value RedactionReason.unknown carries on the wire.
    void redact(EventStore store, long sequenceNumber) {
        EventLogJavaBridge.redact(store.getEventLog(), sequenceNumber, "Unknown");
    }
}
```
