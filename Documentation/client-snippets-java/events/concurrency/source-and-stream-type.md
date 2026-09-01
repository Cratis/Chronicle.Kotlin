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
record ConcurrencyAccountSettingsUpdated(String settings) {}

class EventsConcurrencySourceAndStreamType {
    // Scopes concurrency to a specific event source type and event stream type, in addition to
    // the event source id.
    AppendResult updateAccountSettings(EventStore store, String accountId, String settings) {
        ConcurrencyScope concurrencyScope = ConcurrencyScopeBuilderJavaBridge
            .withSequenceNumber(new ConcurrencyScopeBuilder(), 10)
            .withEventSourceId()
            .withEventSourceType("BankAccount")
            .withEventStreamType("AccountManagement")
            .build();

        AppendOptions options = new AppendOptionsBuilder()
            .eventSourceType("BankAccount")
            .eventStreamType("AccountManagement")
            .concurrencyScope(concurrencyScope)
            .build();

        return EventLogJavaBridge.append(store.getEventLog(), accountId, new ConcurrencyAccountSettingsUpdated(settings), options);
    }
}
```
