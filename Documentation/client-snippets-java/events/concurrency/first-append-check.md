```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendOptions;
import io.cratis.chronicle.eventSequences.AppendResult;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope;
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScopeBuilder;

import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record ConcurrencyFirstAccountOpened(String accountName) {}

class EventsConcurrencyFirstAppendCheck {
    // Opts in to the first-append check with withExpectsNoMatchingEvent(). The kernel rejects the
    // append with a concurrency violation if an event already exists for this event source, so
    // two concurrent "open account" appends cannot both succeed.
    boolean openAccountOnce(EventStore store, String accountId, String accountName) {
        ConcurrencyScope scope = new ConcurrencyScopeBuilder()
            .withEventSourceId()
            .withExpectsNoMatchingEvent()
            .build();

        AppendResult result = EventLogJavaBridge.append(
            store.getEventLog(), accountId, new ConcurrencyFirstAccountOpened(accountName), new AppendOptions(null, scope));

        return result.isSuccess();
    }
}
```
