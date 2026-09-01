```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.EventLogJavaBridge;

class EventsGettingStateTailForEventSource {
    // Scopes the tail sequence number to a specific event source, rather than the whole event log.
    long captureFor(EventStore store, String inventoryId) {
        return EventLogJavaBridge.getTailSequenceNumber(store.getEventLog(), inventoryId);
    }
}
```
