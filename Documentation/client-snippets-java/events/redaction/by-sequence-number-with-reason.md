```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.EventLogJavaBridge;

class EventsRedactionBySequenceNumberWithReason {
    // Redacts a single event by sequence number with a meaningful reason.
    void redact(EventStore store, long sequenceNumber) {
        EventLogJavaBridge.redact(store.getEventLog(), sequenceNumber, "GDPR erasure request");
    }
}
```
