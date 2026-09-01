```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendOptions;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder;

import io.cratis.chronicle.java.AppendOptionsBuilder;
import io.cratis.chronicle.java.ConcurrencyScopeBuilderJavaBridge;
import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record ConcurrencyMonthlyReportGenerated(String month) {}

class EventsConcurrencyStreamId {
    // Scopes concurrency to a specific event stream id within a stream type, so reports for
    // different months don't contend with each other.
    AppendResult generateMonthlyReport(EventStore store, String accountId, String monthKey) {
        ConcurrencyScope concurrencyScope = ConcurrencyScopeBuilderJavaBridge
            .withSequenceNumber(new ConcurrencyScopeBuilder(), 5)
            .withEventSourceId()
            .withEventStreamType("Reporting")
            .withEventStreamId(monthKey)
            .build();

        AppendOptions options = new AppendOptionsBuilder()
            .eventStreamType("Reporting")
            .eventStreamId(monthKey)
            .concurrencyScope(concurrencyScope)
            .build();

        return EventLogJavaBridge.append(store.getEventLog(), accountId, new ConcurrencyMonthlyReportGenerated(monthKey), options);
    }
}
```
