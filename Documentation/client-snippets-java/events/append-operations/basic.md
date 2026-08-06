```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.EventLogJavaBridge;

class EventsAppendOperationsBasic {
    // Observes every append made through this specific IEventLog instance — a hot flow, so only
    // appends made after subscribing are seen.
    void watchAppends(EventStore store) {
        EventLogJavaBridge.watchAppendOperations(store.getEventLog(), entries -> {
            entries.forEach(entry -> System.out.println(
                entry.getEvent().getClass().getSimpleName() + " appended at " +
                EventLogJavaBridge.getSequenceNumber(entry.getResult())));
        });
    }
}
```
