```java
import io.cratis.chronicle.EventStore;

import java.util.List;

import io.cratis.chronicle.java.EventLogJavaBridge;

class EventsCopyingByEventSource {
    // Copies every event of one event source onto another. The copy runs as a durable job on the
    // kernel; the returned job id identifies it.
    String copyAccountHistory(EventStore store, String sourceId, String targetId) {
        return EventLogJavaBridge.copyForEventSource(store.getEventLog(), sourceId, targetId, List.of());
    }
}
```
