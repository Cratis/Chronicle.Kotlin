```java
import io.cratis.chronicle.EventStore;

import java.util.List;

import io.cratis.chronicle.java.EventLogJavaBridge;

class EventsRedactionByEventSource {
    // Redacts every event associated with a particular event source.
    void redactAccount(EventStore store, String eventSourceId) {
        EventLogJavaBridge.redactForEventSource(store.getEventLog(), eventSourceId, "Account deletion requested", List.of());
    }
}
```
