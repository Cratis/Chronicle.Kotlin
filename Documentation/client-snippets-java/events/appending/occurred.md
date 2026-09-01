```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendOptions;
import io.cratis.chronicle.eventSequences.AppendResult;

import java.time.Instant;

import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record OccurredOrderPlaced(String customerId, double total) {}

class EventsAppendingOccurred {
    // Appends an event with an explicit occurred timestamp, bypassing the kernel's default of
    // assigning the current server time. Use this only when importing or replaying historical events.
    AppendResult placeHistoricalOrder(EventStore store, String eventSourceId, String customerId, double total) {
        AppendOptions options = new AppendOptionsBuilder()
            .occurred(Instant.parse("2024-01-15T10:30:00Z"))
            .build();

        return EventLogJavaBridge.append(store.getEventLog(), eventSourceId, new OccurredOrderPlaced(customerId, total), options);
    }
}
```
